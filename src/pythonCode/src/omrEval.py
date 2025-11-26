import torch
import os
import json
from PIL import Image, ImageDraw, ImageFont
import numpy as np
from datetime import timedelta
import math
import time
import socket
import time

from torch.backends import cudnn

from src.LSTMClassifier import LSTMClassifier
from src.Loader import ChannelNormalize
from src.ssd_model import SSD300, Backbone
from src.transforms import Compose, Resize, ToTensor, Normalization

def format(n):
    if len(n) == 1:
        return "0" + n
    return n

def find_box_center(box):
    return [0.5 *(box[0] + box[2]), 0.5 * (box[1] + box[3])]

def dot(v1, v2):
    return v1[0] * v2[0] + v1[1] * v2[1]

def get_head_and_tail_data(numpy_predict_boxes, predict_classes, predict_scores, category_indices, img_size):
    # converting from numpy data type to regular python float
    predict_boxes = []
    for numpy_box in numpy_predict_boxes:
        predict_boxes.append([float(numpy_box[0]), float(numpy_box[1]), float(numpy_box[2]), float(numpy_box[3])])
    # finding ear and tail positions from model
    ear_poses = []
    tail_poses = []
    ear_confs = []
    tail_confs = []
    for i in range(len(predict_boxes)):
        if category_indices[str(predict_classes[i])] == "e":
            ear_poses.append(find_box_center(predict_boxes[i]))
            ear_confs.append(predict_scores[i])
        else:
            tail_poses.append(find_box_center(predict_boxes[i]))
            tail_confs.append(predict_scores[i])

    # thresholding ear and tail values if confidence below 40%
    for i in range(len(ear_poses)):
        if ear_confs[i] < 0.4:
            ear_poses.pop(i)
    for i in range(len(tail_poses)):
        if tail_confs[i] < 0.4:
            tail_poses.pop(i)

    ssd_dict = {
        "ssd_successful": len(ear_poses) >= 2 and len(tail_poses) > 0,
        "head_angle": 0,
        "tail_angle": 0
    }
    if not ssd_dict["ssd_successful"]:
        return ssd_dict
    # print("success: " + str(dict["ssd_successful"]) + ", num ears: " + str(len(ear_poses)) + ", num tails: " + str(len(tail_poses)))

    # selecting the top ear positions with the highest confidence if more than two is recognized by ssd
    if len(ear_poses) > 2:
        conf1, i1 = select_highest(ear_confs)
        ear_confs.pop(i1)
        conf2, i2 = select_highest(ear_confs)
        ear_confs = [conf1, conf2]
        ear_poses = [ear_poses[i1], ear_poses[i2]]
    # selecting top tail position
    if len(tail_poses) > 1:
        conf, i = select_highest(tail_confs)
        tail_confs = [conf]
        tail_poses = [tail_poses[i]]


    # calculating head direction based on ear and tail positions
    ear_vec = [ear_poses[1][0] - ear_poses[0][0], ear_poses[1][1] - ear_poses[0][1]]
    head_vec = [ear_vec[1], -ear_vec[0]]
    ear_mid = find_box_center([*ear_poses[0], *ear_poses[1]])
    rel_tail_vec = [tail_poses[0][0] - ear_mid[0], tail_poses[0][1] - ear_mid[1]]
    if dot(rel_tail_vec, head_vec) > 0:
        head_vec = [head_vec[0] * -1, head_vec[1] * -1]
    tail_vec = [tail_poses[0][0] - img_size[0]*0.5, tail_poses[0][1] - img_size[1]*0.5]

    return {
        "ssd_successful": True,
        "head_angle_rad": math.atan2(head_vec[1], head_vec[0]),
        "tail_angle_rad": math.atan2(tail_vec[1], tail_vec[0]),
        "ear_poses": ear_poses,
        "ear_confs": ear_confs,
        "ears_center": ear_mid,
        "tail_pos": tail_poses[0],
        "tail_conf": tail_confs[0]
    }

def mean(y):
    z = 0
    for x in y:
        z += x
    return z / len(y)
def sign(x):
    if x > 0: return 1
    if x < 0: return -1
    return 0
def select_highest(l):
    highest = 0
    highest_i = 0
    for i in range(len(l)):
        if l[i] > highest:
            highest = l[i]
            highest_i = i
    return highest, highest_i

def run_ssd(model, device, data_transform, original_img):
    transformed_img, _ = data_transform(original_img)
    # expand batch dimension
    transformed_img = torch.unsqueeze(transformed_img, dim=0)

    predictions = model(transformed_img.to(device))[0]  # bboxes_out, labels_out, scores_out
    # predict_boxes is a 2d list; each inner list holds the xmin, ymin, xmax, and ymax for a bounding box, respectively
    predict_boxes = predictions[0].to("cpu").numpy()
    predict_classes = predictions[1].to("cpu").numpy()
    predict_scores = predictions[2].to("cpu").numpy()
    # re-scaling bounding boxes to normal screen dimensions
    predict_boxes[:, [0, 2]] = predict_boxes[:, [0, 2]] * original_img.size[0]
    predict_boxes[:, [1, 3]] = predict_boxes[:, [1, 3]] * original_img.size[1]

    return predict_boxes, predict_classes, predict_scores


# shifts result one to the left and adds new onto
def shift_results(r, index, new):
    r[index, :-1] = r[index, 1:].clone()
    r[index, -1] = new
    return r
def analyze_camera_img(img_i, original_img, model, ssd_input_transform, ci, lstm, lstm_input_transform, results, window_size, angle_offsets_rad, fps, device):
    # print("time to open img " + str(img_i) + ": " + str(time.time() - before))
    w = original_img.size[0]
    h = original_img.size[1]

    # running model and analyzing model predictions
    pred_box, pred_class, pred_score = run_ssd(model, device, ssd_input_transform, original_img)
    data = get_head_and_tail_data(pred_box, pred_class, pred_score, ci, original_img.size)

    # drawing some data onto img
    cx = w * 0.8
    cy = h * 0.8
    rh = h * 0.2
    rw = w * 0.37
    top = cy - rh * 0.6
    text_left = cx - rw*0.46
    text_inc = rh/5.7
    draw = ImageDraw.Draw(original_img)
    line_width = 2
    font_size = 20
    fnt = ImageFont.truetype("arialbd.ttf", font_size)
    draw.rectangle((cx - rw/2, cy-rh/2, cx+rw/2, cy+rh/2), fill=None, outline=(0, 10, 200), width=line_width)
    draw.text(xy=(text_left, top+text_inc), text="Frame: " + str(img_i), font=fnt, fill="black")

    total_seconds = img_i/fps
    hours = int(total_seconds // 3600)
    minutes = int((total_seconds % 3600) // 60)
    seconds = int(total_seconds % 60)
    formatted = f"{format(str(hours))}:{format(str(minutes))}:{format(str(seconds))}"
    draw.text(xy=(text_left, top+text_inc*2), text="Time: " + formatted, font=fnt, fill="black")

    cur_i = min(img_i, window_size - 1)
    # print("ssd successful: " + str(data["ssd_successful"]))
    # copying last frames data if ssd fails; if it fails on first frame, then it takes the default values
    print("ssdSuccess: " + str(data["ssd_successful"]))
    if not data["ssd_successful"]:
        if img_i == 0:
            results[0, img_i] = 0
            results[1, img_i] = 0
        elif img_i < window_size:
            results[0, img_i] = results[0, img_i - 1]
            results[1, img_i] = results[1, img_i - 1]
        else:
            shift_results(results, 0, results[0, -1])
            shift_results(results, 1, results[1, -1])
    else:
        # drawing ear and tail positions
        box_r = 15
        label_w = font_size * 3
        label_h = font_size
        green = (40, 255, 40)
        for i, ear in enumerate(data["ear_poses"]):
            draw.rectangle(xy=(ear[0]-box_r, ear[1] - box_r, ear[0] + box_r, ear[1] + box_r), fill=None, outline=green, width=line_width)
            draw.rectangle(xy=(ear[0]-box_r, ear[1]-box_r-label_h, ear[0]-box_r+label_w, ear[1]-box_r), fill=green)
            draw.text(xy=(ear[0]-box_r, ear[1]-box_r-label_h), text="e: " + str(math.floor(data["ear_confs"][i]*100)) + "%", font=fnt, fill="black")
        tail = data["tail_pos"]
        blue = (40, 230, 255)
        draw.rectangle(xy=(tail[0]-box_r, tail[1] - box_r, tail[0] + box_r, tail[1] + box_r), fill=None, outline=blue, width=line_width)
        draw.rectangle(xy=(tail[0]-box_r, tail[1]-box_r-label_h, tail[0]-box_r+label_w, tail[1]-box_r), fill=blue)
        draw.text(xy=(tail[0]-box_r, tail[1]-box_r-label_h), text="t: " + str(math.floor(data["tail_conf"]*100)) + "%", font=fnt, fill="black")

        # correcting head and tail angle wraparound issue
        # because atan2 returns angles on [-pi,pi] so there is a cut-off at pi radians
        if img_i > 0:
            data["head_angle_rad"] += angle_offsets_rad[0]
            data["tail_angle_rad"] += angle_offsets_rad[1]

            dif = data["head_angle_rad"] - results[0, cur_i-1]
            if abs(dif) > angle_offsets_rad[2]:
                offset = -sign(dif) * 2 * math.pi
                angle_offsets_rad[0] += offset
                data["head_angle_rad"] += offset

            dif = data["tail_angle_rad"] - results[1, cur_i - 1]
            if abs(dif) > angle_offsets_rad[2]:
                offset = -sign(dif) * 360
                angle_offsets_rad[1] += offset
                data["tail_angle_rad"] += offset

        if img_i < window_size:
            results[0, img_i] = data["head_angle_rad"]
            results[1, img_i] = data["tail_angle_rad"]
        else:
            shift_results(results, 0, data["head_angle_rad"])
            shift_results(results, 1, data["tail_angle_rad"])

    # drawing head and tail angle
    draw.text(xy=(text_left, top+text_inc*3), text="Head Angle: " + str(math.floor(results[0, cur_i].item() * 180/math.pi * 100)/100), font=fnt, fill="black")
    draw.text(xy=(text_left, top+text_inc*4), text="Tail Angle: " + str(math.floor(results[1, cur_i].item() * 180/math.pi * 100)/100), font=fnt, fill="black")

    # print("starting sliding window")
    # sliding window approach
    head_results_deg = results[0] * 180 / math.pi
    tail_results_deg = results[1] * 180 / math.pi

    head_mean = torch.mean(head_results_deg)
    head_window = (head_results_deg - head_mean)

    head_std = torch.std(head_window)
    head_window = (head_window-torch.mean(head_window)) / head_std

    tail_mean = torch.mean(tail_results_deg)
    tail_window = (tail_results_deg - tail_mean)
    tail_std = torch.std(tail_window)
    tail_window = (tail_window - torch.mean(tail_window)) / tail_std

    print("img_i" + str(img_i))
    # print("H Mean: " + str(head_mean))
    # print("H Std: " + str(head_std))
    # print("HEAD DEG: " + str(head_results_deg))
    # print(head_window)
    # print("TAIL Mean: " + str(tail_mean))
    # print("TAIL STD: " + str(tail_std))
    # print("TAIL DEG: " + str(tail_results_deg))

    # plt.plot(head_window)

    # time = torch.arange(48)  # 0, 1, 2, ..., 99
    #
    # # Plotting
    # plt.figure(figsize=(10, 5))
    # plt.plot(time.numpy(), head_window.numpy(), label='Signal')
    # plt.xlabel('Time')
    # plt.ylabel('Value')
    #
    # plt.savefig(r"C:\Users\justi\Documents\omr images\raw images\fig.png")
    # plt.show()
    # quit()
    # print(tail_window)
    head_window = head_window.unsqueeze(-1)
    tail_window = tail_window.unsqueeze(-1)
    signal = torch.cat([head_window, tail_window], dim=-1)
    signal = lstm_input_transform(signal).to(device)
    signal = signal.unsqueeze(0)
    preds = lstm(signal)
    x = w * 0.15
    pred = preds.cpu().squeeze()
    if pred > 0.5:
        red = (250, 20, 5)
        draw.text(xy=(text_left, top+text_inc*5), text="OMR DETECTED", font=fnt, fill=red)

    results[2, 0] = pred
    print("------LSTM: " + str(pred))
    # print("total time to process img " + str(img_i) + ": " + str(time.time() - a))

    return original_img

def connect_to_server(host, port):
    while True:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((host, port))
            print(f"python socket connected at {host}:{port}")
            return s
        except ConnectionRefusedError:
            print("java server not ready yet, waiting 0.1s")
            time.sleep(0.1)

if __name__ == "__main__":
    # necessary file paths and variables
    pascal_voc_path = r"C:\Users\justi\Documents\GitHub\OMRProject\src\pythonCode\model\pascal_voc_classes.json"
    ssd_model_path = r"C:\Users\justi\Documents\GitHub\OMRProject\src\pythonCode\model\ssd.pth"
    lstm_model_path = r"C:\Users\justi\Documents\GitHub\OMRProject\src\pythonCode\model\best.pkl"

    HOST = "127.0.0.1"
    RECEIVE_PORT = 65433
    SEND_PORT = 65432

    cudnn.benchmark = False
    model_device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")  # using GPU if possible
    print("using " + str(model_device))

    # loading ssd
    ssd_model = SSD300(backbone=Backbone(), num_classes=3)
    weights_dict = torch.load(ssd_model_path, map_location=model_device)
    weights_dict = weights_dict["model"] if "model" in weights_dict else weights_dict
    ssd_model.load_state_dict(weights_dict)
    ssd_model.to(model_device)
    ssd_model.eval()
    # loading ssd class dict
    assert os.path.exists(pascal_voc_path), "file '{}' dose not exist.".format(pascal_voc_path)
    json_file = open(pascal_voc_path, 'r')
    class_dict = json.load(json_file)
    json_file.close()
    category_index = {str(v): str(k) for k, v in class_dict.items()}
    # transformation to normalize images for ssd input
    ssd_transform = Compose([Resize(),
                             ToTensor(),
                             Normalization()])
    print("ssd loaded")

    # loading lstm
    lstm_model = LSTMClassifier().to(model_device)
    checkpoint = torch.load(lstm_model_path, map_location=model_device)
    lstm_model.load_state_dict(checkpoint["model_state_dict"])
    lstm_model.to(model_device)
    lstm_model.eval()
    # normalizes lstm input
    lstm_transform = ChannelNormalize()
    print("lstm loaded")

    # lstm preprocessing settings
    lstm_window_size = 48
    analysis_results = torch.zeros((3, lstm_window_size)) # stores head and tail angles and whether omr is detected at specific frame BUT: FOR OMR DETECTION I JUST OVERWRITE VALUES AND ONLY STORE CURRENT PREDICTION AT [2,0]
    angle_offset_data_rad = torch.tensor([0, 0, 5]) # 0 is head offsets, 1 is tail offsets, 2 is threshold to offset

    use_sockets = False
    no_socket_img_path = r"C:\Users\justi\Documents\omr images\raw images\wt1_cw_highw_lowspeed" # file path that program resorts to in order to get images when no sockets are supposed to be used
    no_socket_output_img_path = r"C:\Users\justi\Documents\omr images\raw images\testingOutput"
    no_socket_img_num = 60

    if use_sockets:
        # initializing socket connections
        receive_socket = connect_to_server(HOST, RECEIVE_PORT)
        send_socket = connect_to_server(HOST, SEND_PORT)

        # system variables
        header = receive_socket.recv(12)
        img_width = int.from_bytes(header[0:4], "big")
        img_height = int.from_bytes(header[4:8], "big")
        num_img_bytes = img_width * img_height * 3
        camera_fps = int.from_bytes(header[8:12], "big")
        print(f"SOCKET camera data received width: {img_width} height: {img_height}")
    else:
        camera_fps = 24
        print("not using sockets; default fps=24, getting images from " + no_socket_img_path)


    with torch.no_grad():
        # warming up model
        init_img = torch.zeros((1, 3, 300, 300), device=model_device)
        ssd_model(init_img)

        while True:
            if use_sockets:
                image_num_byte = receive_socket.recv(4)
                timeA = time.time()
                image_i = int.from_bytes(image_num_byte, "big") - 1

                byte_data = b''
                while len(byte_data) < num_img_bytes:
                    # print("Expected bytes:", num_img_bytes)
                    # print("Received bytes:", len(byte_data))
                    packet = receive_socket.recv(num_img_bytes - len(byte_data))
                    byte_data += packet
                receiveArr = np.frombuffer(byte_data, dtype=np.uint8).reshape((img_height, img_width, 3))
                receiveArr = receiveArr[:, :, ::-1]
                img = Image.fromarray(receiveArr)
            else:
                image_i = no_socket_img_num - 1
                img_path = os.path.join(no_socket_img_path, str(no_socket_img_num) + ".png")
                print("img_path: " + img_path)
                img = Image.open(img_path)



            if image_i == 0:
                analysis_results = torch.zeros((3, lstm_window_size))

            img = analyze_camera_img(image_i, img, ssd_model, ssd_transform, category_index, lstm_model, lstm_transform, analysis_results, lstm_window_size, angle_offset_data_rad, camera_fps, model_device)

            # i send back the annotated image and whether it shows omr or not
            if use_sockets:
                sendArr = np.array(img, dtype=np.uint8)
                send_socket.sendall(round(analysis_results[2, 0].item()).to_bytes(4, "big"))
                send_socket.sendall(sendArr.tobytes())

            # updating no socket logic
            else:
                img_str = str(no_socket_img_num)
                while len(img_str) < 5:
                    img_str = "0" + img_str
                img.save(os.path.join(no_socket_output_img_path, "vis" + img_str + ".png"))
                no_socket_img_num += 1



            # print("imgI: " + str(image_i) + " | lstm: " + str(analysis_results[2, 0]) + " | angle Offs: " + str(angle_offset_data_rad))


            # window frame updating is correct
            # units is correct
            # mean and standard deviation i assume are correct?
