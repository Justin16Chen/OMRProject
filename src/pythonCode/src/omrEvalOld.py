import cv2
from src.transforms import *
from src.LSTMClassifier import LSTMClassifier
from src.Loader import ChannelNormalize
from src.ssd_model import SSD300, Backbone
import torch
import os
import json
from PIL import Image, ImageDraw, ImageFont
import math
import numpy as np
from datetime import datetime, timedelta
import math
import time
import socket
import io
import struct



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

    dict = {
        "ssd_successful": len(ear_poses) >= 2 and len(tail_poses) > 0,
        "head_angle": 0,
        "tail_angle": 0
    }
    if not dict["ssd_successful"]:
        return dict
    # print("success: " + str(dict["ssd_successful"]) + ", num ears: " + str(len(ear_poses)) + ", num tails: " + str(len(tail_poses)))
    print()
    if not dict["ssd_successful"]:
        return dict

    # selecting the top ear positions with highest confidence if more than two is recognized by ssd
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
        "head_angle": math.atan2(head_vec[1], head_vec[0]),
        "tail_angle": math.atan2(tail_vec[1], tail_vec[0]),
        "ear_poses": ear_poses,
        "ear_confs": ear_confs,
        "ears_center": ear_mid,
        "tail_pos": tail_poses[0],
        "tail_conf": tail_confs[0]
    }

def compensate_angle(ssd_outputs, current_index, output_index, phase_compensate_threshold):
    angle_dif = ssd_outputs[current_index + 1][output_index] - ssd_outputs[current_index][output_index]
    if abs(angle_dif) >= phase_compensate_threshold:
        offset_amount = math.pi * 2 * -sign(angle_dif)
        for j in range(current_index + 1, len(ssd_outputs)):
            ssd_outputs[j][output_index] += offset_amount
def mean(y):
    z = 0
    for x in y:
        z += x
    return z / len(y)
def sign(x):
    if x > 0: return 1
    if x < 0: return -1
    return 0
def select_highest(list):
    highest = 0
    highest_i = 0
    for i in range(len(list)):
        if list[i] > highest:
            highest = list[i]
            highest_i = i
    return highest, highest_i

def run_ssd(model, data_transform, original_img):
    img, _ = data_transform(original_img)
    # expand batch dimension
    img = torch.unsqueeze(img, dim=0)

    predictions = model(img.to(device))[0]  # bboxes_out, labels_out, scores_out
    # predict_boxes is a 2d list; each inner list holds the xmin, ymin, xmax, and ymax for a bounding box, respectively
    predict_boxes = predictions[0].to("cpu").numpy()
    predict_classes = predictions[1].to("cpu").numpy()
    predict_scores = predictions[2].to("cpu").numpy()
    # re-scaling bounding boxes to normal screen dimensions
    predict_boxes[:, [0, 2]] = predict_boxes[:, [0, 2]] * original_img.size[0]
    predict_boxes[:, [1, 3]] = predict_boxes[:, [1, 3]] * original_img.size[1]

    return predict_boxes, predict_classes, predict_scores
def analyze_camera_img(img_i, original_img, model, ssd_input_transform, category_index, lstm, lstm_input_transform, results, window_size, angle_offsets, fps, device):
    a = time.time()
    before = time.time()
    # print("time to open img " + str(img_i) + ": " + str(time.time() - before))
    w = original_img.size[0]
    h = original_img.size[1]

    # running model and analyzing model predictions
    pred_box, pred_class, pred_score = run_ssd(model, ssd_input_transform, original_img)
    data = get_head_and_tail_data(pred_box, pred_class, pred_score, category_index, original_img.size)

    # drawing some data onto img
    cx = w * 0.8
    cy = h * 0.8
    rh = h * 0.2
    rw = w * 0.37
    top = cy - rh/2
    text_left = cx - rw*0.45
    text_inc = rh/5.8
    draw = ImageDraw.Draw(original_img)
    line_width=2
    font_size = 20
    fnt = ImageFont.truetype("arialbd.ttf", font_size)
    draw.rectangle((cx - rw/2, cy-rh/2, cx+rw/2, cy+rh/2), fill=None, outline=(0, 10, 200), width=line_width)
    draw.text(xy=(text_left, top+text_inc), text="Frame: " + str(img_i), font=fnt, fill="black")
    draw.text(xy=(text_left, top+text_inc*2), text="Time: " + str(timedelta(seconds=img_i/fps)), font=fnt, fill="black")

    # print("ssd successful: " + str(data["ssd_successful"]))
    # copying last frames data if ssd fails; if it fails on first frame, then it takes the default values
    if not data["ssd_successful"]:
        if img_i > 0:
            results[0, img_i] = results[0, img_i - 1]
            results[1, img_i] = results[1, img_i - 1]
        else:
            results[0, img_i] = data["head_angle"]
            results[1, img_i] = data["tail_angle"]
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

        # converting to degrees
        data["head_angle"] *= 180 / math.pi
        data["tail_angle"] *= 180 / math.pi

        # correcting head and tail angle wraparound issue
        if img_i > 0:
            data["head_angle"] += angle_offsets[0]
            data["tail_angle"] += angle_offsets[1]

            dif = data["head_angle"] - results[0, img_i-1]
            if abs(dif) > angle_offsets[2]:
                offset = -sign(dif) * 360
                angle_offsets[0] += offset
                data["head_angle"] += offset

            dif = data["tail_angle"] - results[1, img_i - 1]
            if abs(dif) > angle_offsets[2]:
                offset = -sign(dif) * 360
                angle_offsets[1] += offset
                data["tail_angle"] += offset

        results[0, img_i] = data["head_angle"]
        results[1, img_i] = data["tail_angle"]

    # drawing head and tail angle
    draw.text(xy=(text_left, top+text_inc*3), text="Head Angle (deg): " + str(math.floor(results[0, img_i].item()*100)/100), font=fnt, fill="black")
    draw.text(xy=(text_left, top+text_inc*4), text="Tail Angle (deg): " + str(math.floor(results[1, img_i].item())*100/100), font=fnt, fill="black")

    # print("starting sliding window")
    # sliding window approach
    start_i = int(max(0, img_i + 1 - window_size))
    head_window = results[0, start_i:img_i + 1]
    tail_window = results[1, start_i:img_i + 1]
    if img_i < window_size - 1:
        pad = window_size - img_i - 1
        head_window = torch.cat([torch.zeros(pad), head_window], dim=0)
        tail_window = torch.cat([torch.zeros(pad), tail_window], dim=0)
    head_window = head_window.unsqueeze(-1)
    tail_window = tail_window.unsqueeze(-1)
    signal = torch.cat([head_window, tail_window], dim=-1)
    signal = lstm_input_transform(signal).to(device)
    signal = signal.unsqueeze(0)
    preds = lstm(signal)
    x = w * 0.15
    if preds.cpu().numpy().squeeze() > 0.5:
        results[2, img_i] = 1
        red = (250, 20, 5)
        draw.rectangle(xy=(50, 500, 100, 520), fill=None, outline=red, width=line_width)
        draw.text(xy=(x, cy), text="OMR", font=fnt, fill=red)
    else:
        results[2, img_i] = 0
        if img_i > 0 and results[2, img_i-1] == 1:
            results[3, 0] += 1
    # print("total time to process img " + str(img_i) + ": " + str(time.time() - a))

    return original_img

def reset_experiments(program_info_dict, program_json_path):
    program_info_dict["experiments"] = []
    with open(program_json_path, "w") as file:
        json.dump(program_info_dict, file, indent=4)

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
    # necessary file paths
    program_json_path = "../../liveData/programInfo.json"
    pascal_voc_path = "model/pascal_voc_classes.json"
    ssd_model_path = "model/ssd.pth"
    lstm_model_path = "model/lstm.pkl"
    omr_output_path = "../../liveData/omrData.json"

    # clearing previous omr data
    with open(omr_output_path, "w") as file:
        json.dump({"experiments": []}, file, indent=4)

    # loading ssd
    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")  # using GPU if possible
    print("using " + str(device))

    ssd = SSD300(backbone=Backbone(), num_classes=3)
    weights_dict = torch.load(ssd_model_path, map_location=device)
    weights_dict = weights_dict["model"] if "model" in weights_dict else weights_dict
    ssd.load_state_dict(weights_dict)
    ssd.to(device)
    ssd.eval()
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
    lstm = LSTMClassifier().to(device)
    checkpoint = torch.load(lstm_model_path, map_location=device)
    lstm.load_state_dict(checkpoint["model_state_dict"])
    lstm.to(device)
    lstm.eval()
    # normalizes lstm input
    lstm_transform = ChannelNormalize()
    print("lstm loaded")

    # declaring system variables
    fps = 0
    with open(program_json_path, "r") as file:
        json_file = json.load(file)
        fps = json_file["fps"]
        HOST = json_file["HOST"]
        RECEIVE_PORT = json_file["PYTHON_RECEIVE_PORT"]
        SEND_PORT = json_file["PYTHON_SEND_PORT"]
    spf = 1/fps

    program_state = "inactive"
    experiment_name = ""
    trial_prefix = json_file["trialPrefix"]

    camera_output_base = ""
    camera_output_path = ""

    visualized_output_base = ""
    visualized_output_folder = ""


    # time for ssd and lstm is 0.36 on cpu

    experiment_i = -1
    max_experiments = 0
    trial_i = -1

    next_img = 0 # name of next image that needs to be analyzed
    max_img = 0 # max number of images per trial
    images = [] # pil images to save to file path at end of every trial

    window_size = 48
    results = torch.zeros((4, max_img)) # stores head and tail angles, whether omr is detected at specific frame, and how many omr occurrences total so far
    angle_offsets = torch.tensor([0, 0, 5*180/math.pi]) # 0 is head offsets, 1 is tail offsets, 2 is threshold to offset

    # initializing socket connections
    receive_socket = connect_to_server(HOST, RECEIVE_PORT)
    send_socket = connect_to_server(HOST, SEND_PORT)

    with torch.no_grad():
        # warming up model
        init_img = torch.zeros((1, 3, 300, 300), device=device)
        ssd(init_img)

        prev_time = time.time()
        while True:
            timeA = time.time()

            header = receive_socket.recv(8)
            width = int.from_bytes(header[0:4], "big")
            height = int.from_bytes(header[4:8], "big")

            img_bytes = receive_socket.recv(width * height * 3)
            arr = np.frombuffer(img_bytes, dtype-np.uint8).reshape((height, width, 3))
            arr = arr[:, :, ::-1]
            img = Image.fromarray(arr, 'RGB')

            img = analyze_camera_img(next_img, img, ssd, ssd_transform, category_index, lstm, lstm_transform, results, window_size, angle_offsets, fps, device)

            arr = np.array(rgb_img, dtype=np.uint8)
            # print("time to convert img: " + str(time.time() - timeA))

            height, width, _ = arr.shape
            header = width.to_bytes(4, "big") + height.to_bytes(4, "big")
            # print("time to bytes: " + str(time.time() - timeA))

            send_socket.sendall(header)
            send_socket.sendall(arr.tobytes())

            # ensuring fps matches with program fps
            # dt = time.time() - prev_time
            # if dt < spf:
            #     time.sleep(spf - dt)
            # prev_time = time.time()
"""
            # print(program_state + ", expI:" + str(experiment_i) + ", trialI: " + str(trial_i) + ", imgI: " + str(next_img))
            with open(program_json_path, "r") as file:
                program_info_dict = json.load(file)

            if not program_info_dict["programRunning"]:
                receive_socket.close()
                send_socket.close()
                print("programRunning in programJson = false; stopping python program")
                quit()

            # basic state machine
            if program_state == "inactive":
                experiments = program_info_dict["experiments"]
                # updating experiment variables for first experiment
                if len(experiments) > 0:
                    experiment_i = 0
                    max_experiments = len(experiments)
                    trial_i = 0
                    next_img = 0
                    max_img = experiments[0]["expectedImages"]
                    results = torch.zeros(4, max_img)

                    experiment_name = experiments[0]["name"]
                    # these paths are specific to the current trial that is running
                    camera_output_base = program_info_dict["cameraOutputBase"]
                    camera_output_path = os.path.join(camera_output_base, experiment_name, trial_prefix + str(0))
                    visualized_output_base = program_info_dict["visualizedOutputBase"]
                    visualized_output_folder = os.path.join(visualized_output_base, experiment_name, trial_prefix + str(0))
                    program_state = "active"

            elif program_state == "active":
                if program_info_dict["stopEarly"]:
                    program_state = "inactive"
                    reset_experiments(program_info_dict, program_json_path)
                    experiment_i = -1
                    trial_i = -1
                    images = []
                elif next_img < max_img: # means that I am in the middle of a trial
                    # img_path = os.path.join(camera_output_path, str(next_img) + ".png")
                    # if not os.path.exists(img_path):
                    #     continue
                    # img = Image.open(img_path)
                    header = receive_socket.recv(8)
                    width = int.from_bytes(header[0:4], "big")
                    height = int.from_bytes(header[4:8], "big")
                    img_bytes = receive_socket.recv(width * height * 3)
                    arr = np.frombuffer(img_bytes, dtype-np.uint8).reshape((height, width, channels))
                    arr = arr[:, :, ::-1]
                    img = Image.fromarray(arr, 'RGB')
                    img = analyze_camera_img(next_img, img, ssd, ssd_transform, category_index, lstm, lstm_transform, results, window_size, angle_offsets, fps, device)

                    timeA = time.time()
                    next_img += 1
                    images.append(img)

                    rgb_img = img.convert("RGB")
                    arr = np.array(rgb_img, dtype=np.uint8)
                    # print("time to convert img: " + str(time.time() - timeA))

                    height, width, channels = arr.shape
                    header = width.to_bytes(4, "big") + height.to_bytes(4, "big")
                    # print("time to bytes: " + str(time.time() - timeA))

                    send_socket.sendall(header)
                    send_socket.sendall(arr.tobytes())
                    # print("time to send img: " + str(time.time() - timeA))


                else: # means that I am done analyzing a trial
                    next_camera_output_path = os.path.join(camera_output_base, experiment_name, trial_prefix + str(trial_i + 1))
                    next_trial_exists = os.path.exists(next_camera_output_path)
                    experiment_complete = program_info_dict["experiments"][experiment_i]["completed"]

                    # checking if in the 'waiting period' -> waiting for another trial or for experiment to be marked as completed
                    if not next_trial_exists and not experiment_complete:
                        continue

                    # saving omr data of last analyzed trial if not in waiting period
                    with open(omr_output_path, "r") as file:
                        omr_dict = json.load(file)
                    if trial_i == 0:
                        omr_dict["experiments"].append({
                            "name": experiment_name,
                            "omr": [results[3, 0].item()]
                        })
                    else:
                        omr_dict["experiments"][experiment_i]["omr"].append(results[3, 0].item())
                    with open(omr_output_path, "w") as file:
                        json.dump(omr_dict, file, indent=4)

                    if next_trial_exists:
                        trial_i += 1
                    # code below will only run if experiment has been completed
                    elif experiment_complete:
                        if experiment_i + 1 >= max_experiments: # means that there are no more experiments
                            program_state = "inactive"
                            reset_experiments(program_info_dict, program_json_path)
                            experiment_i = -1
                            trial_i = -1
                        else: # means that I should progress to the next experiment
                            experiment_i += 1
                            experiment_name = program_info_dict["experiments"][experiment_i]["name"]
                            trial_i = 0
                            max_img = program_info_dict["experiments"][experiment_i]["expectedImages"]

                    # uploading trial images
                    for i in range(len(images)):
                        path = os.path.join(visualized_output_folder, str(i) + ".png")
                        images[i].save(path)
                    print("images saved at " + visualized_output_folder)

                    # setting up for next trial
                    images = []
                    next_img = 0
                    results = torch.zeros(4, max_img)
                    visualized_output_folder = os.path.join(visualized_output_base, experiment_name, trial_prefix + str(trial_i))
                    camera_output_path = os.path.join(camera_output_base, experiment_name, trial_prefix + str(trial_i))
"""