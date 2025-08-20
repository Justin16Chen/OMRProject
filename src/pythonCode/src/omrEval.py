from src import transforms
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

def find_box_center(box):
    return [0.5 *(box[0] + box[2]), 0.5 * (box[1] + box[3])]

def dot(v1, v2):
    return v1[0] * v2[0] + v1[1] * v2[1]

#
# # these two functions are outdated
# def run_ssd_eval(should_display_image):
#     visualization_output_folder_path = ("../../liveData/ssdVisualizedImages")
#     image_folder_path = "../../liveData/cameraImages"
#     json_path = "model/pascal_voc_classes.json"
#     model_path = "model/ssd.pth"
#
#     # loading model
#     device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")  # using GPU if possible
#     print("starting SSD eval on {}".format(device))
#     model = SSD300(backbone=Backbone(), num_classes=3)
#     weights_dict = torch.load(model_path, map_location='cpu')
#     weights_dict = weights_dict["model"] if "model" in weights_dict else weights_dict
#     model.load_state_dict(weights_dict)
#     model.to(device)
#
#     # loading model inputs
#     images = os.listdir(image_folder_path)
#     if len(images) == 0:
#         raise Exception("on images provided for model to evaluate")
#
#     assert os.path.exists(json_path), "file '{}' dose not exist.".format(json_path)
#     json_file = open(json_path, 'r')
#     class_dict = json.load(json_file)
#     json_file.close()
#     category_index = {str(v): str(k) for k, v in class_dict.items()}
#
#     data_transform = transforms.Compose([transforms.Resize(),
#                                          transforms.ToTensor(),
#                                          transforms.Normalization()])
#     results = []
#
#     # running evaluations
#     model.eval()
#     with torch.no_grad():
#         # used to warm up model
#         init_img = torch.zeros((1, 3, 300, 300), device=device)
#         model(init_img)
#
#         # looping through images
#         for index in range(0, len(os.listdir(image_folder_path)) - 1):
#             # print("\nindex {} | starting image evaluation".format(index))
#
#             # load current image
#             img_Path = os.path.join(image_folder_path, str(index) + '.png')
#             original_img = Image.open(img_Path)
#             img, _ = data_transform(original_img)
#             # expand batch dimension
#             img = torch.unsqueeze(img, dim=0)
#
#             predictions = model(img.to(device))[0]  # bboxes_out, labels_out, scores_out
#             # predict_boxes is a 2d list; each inner list holds the xmin, ymin, xmax, and ymax for a bounding box, respectively
#             predict_boxes = predictions[0].to("cpu").numpy()
#             predict_classes = predictions[1].to("cpu").numpy()
#             predict_scores = predictions[2].to("cpu").numpy()
#             # re-scaling model outputs to normal screen dimensions
#             predict_boxes[:, [0, 2]] = predict_boxes[:, [0, 2]] * original_img.size[0]
#             predict_boxes[:, [1, 3]] = predict_boxes[:, [1, 3]] * original_img.size[1]
#
#             # analyzing model predictions
#             mouse_data = get_head_and_tail_data(predict_boxes, predict_classes, category_index, original_img.size)
#             if not mouse_data["ssd_successful"]:
#                 continue
#             # saving data
#             results.append([mouse_data['head_angle'], mouse_data['tail_angle']])
#
#             # visualizing image
#             if should_display_image:
#                 plot_img = draw_objs(original_img,
#                                      predict_boxes[:, :],
#                                      predict_classes[:],
#                                      predict_scores[:],
#                                      category_index=category_index,
#                                      box_thresh=0.5,
#                                      line_thickness=3,
#                                      font='arial.ttf',
#                                      font_size=20,
#                                      draw_boxes_on_image=True,
#                                      mouse_data=mouse_data)
#                 output_path = os.path.join(visualization_output_folder_path, str(index) + ".png")
#                 plot_img.save(output_path)
#
#     print("num results: " + str(len(results)))
#     return results
#
# def process_ssd_outputs(ssd_outputs, phase_compensate_threshold, window_size, fps):
#
#     # index 0 - corrected head angles (degrees)
#     # index 1 - corrected tail angles (degrees)
#     # index 2 - slopes of best fit lines for head angles over all windows (deg/s)
#     # index 3 - head angle residuals
#     # index 4 - slopes for tail angles (deg/s)
#     # index 5 - tail angle residuals
#     results = [[], [], [], [], [], []]
#
#
#     # fixing transition issue from 0 to 2pi of head and tail angles
#     for i in range(0, len(ssd_outputs)-1):
#         compensate_angle(ssd_outputs, i, 0, phase_compensate_threshold)
#         compensate_angle(ssd_outputs, i, 1, phase_compensate_threshold)
#     # converting to degrees and storing values in results
#     for i in range(len(ssd_outputs)):
#         results[0].append(ssd_outputs[i][0] * 180 / math.pi)
#         results[1].append(ssd_outputs[i][1] * 180 / math.pi)
#     # sliding window approach to obtain slopes of best fit lines and residuals of best fit lines over window
#     for i in range(2, len(results[0])+1):
#         head_window = results[0][int(max(0, i-window_size)):i]
#         p_head, head_residual = get_window_data(head_window, fps)
#
#         results[2].append(float(p_head[0]))
#         results[3].append(float(head_residual))
#
#         tail_window = results[1][int(max(0, i-window_size)):i]
#         p_tail, tail_residual = get_window_data(tail_window, fps)
#
#         results[4].append(float(p_tail[0]))
#         results[5].append(float(tail_residual))
#
#     print('ssd outputs processed')
#     return results



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
        ear_confs.pop(highest)
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


# def get_window_data(window, fps):
#     avg = mean(window)
#     window = [value - avg for value in window]
#     x_values = [x / fps for x in range(len(window))]
#     p = np.polyfit(x_values, window, 1)
#     y_fit = np.polyval(p, x_values)
#     residuals = window - y_fit
#     normr = np.linalg.norm(residuals)
#     return p, normr

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
def analyze_camera_img(img_i, model, ssd_input_transform, category_index, lstm, lstm_input_transform, results, window_size, angle_offsets, image_folder_path, visualization_folder_path, fps, device):
    # print("img_i: " + str(img_i))
    img_path = image_folder_path + "/" + str(img_i) + '.png'
    if not os.path.exists(img_path):
        print("path " + img_path + " doesn't exist")
        return False
    original_img = Image.open(img_path)
    w = original_img.size[0]
    h = original_img.size[1]
    img_output_path = os.path.join(visualization_folder_path, str(img_i) + ".png")

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
        results[2][img_i] = 1
        red = (250, 20, 5)
        draw.rectangle(xy=(50, 500, 100, 520), fill=None, outline=red, width=line_width)
        draw.text(xy=(x, cy), text="OMR", font=fnt, fill=red)
    else:
        results[2][img_i] = 0
        if img_i > 0 and results[2][img_i-1] == 1:
            results[3][0] += 1

    original_img.save(img_output_path)
    return True

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
    ssd_transform = transforms.Compose([transforms.Resize(),
                                        transforms.ToTensor(),
                                        transforms.Normalization()])
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
        fps = json.load(file)["fps"]
    spf = 1/fps
    print("fps: " + str(fps))



    program_state = "inactive"
    experiment_name = ""
    trial_prefix = "trial"

    camera_output_base = ""
    camera_output_path = ""

    visualized_output_base = ""
    visualized_output_path = ""


    experiment_i = -1
    max_experiments = 0
    trial_i = -1

    next_img = 0 # name of next image that needs to be analyzed
    max_img = 0 # max number of images per trial

    window_size = 48
    results = torch.zeros((4, max_img)) # stores head and tail angles, whether omr is detected at specific frame, and how many omr occurrences total so far
    angle_offsets = torch.tensor([0, 0, 5*180/math.pi]) # 0 is head offsets, 1 is tail offsets, 2 is threshold to offset

    with torch.no_grad():
        # warming up model
        init_img = torch.zeros((1, 3, 300, 300), device=device)
        ssd(init_img)

        prev_time = datetime.now()
        while True:
            # ensuring fps matches with program fps
            dif = datetime.now() - prev_time
            if dif.total_seconds() < spf:
                continue
            string = program_state + ", exi: " + str(experiment_i) + ", ti: " + str(trial_i) + ", imgi: " + str(next_img)
            prev_time = datetime.now()

            # basic state machine
            if program_state == "inactive":
                with open(program_json_path, "r") as file:

                    dict = json.load(file)
                    experiments = dict["experiments"]
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
                    camera_output_base = dict["cameraOutputBase"]
                    camera_output_path = os.path.join(camera_output_base, experiment_name, trial_prefix + str(0))
                    visualized_output_base = dict["visualizedOutputBase"]
                    visualized_output_path = os.path.join(visualized_output_base, experiment_name, trial_prefix + str(0))
                    program_state = "active"

            elif program_state == "active":
                if next_img < max_img: # means that I am in the middle of a trial
                    if analyze_camera_img(next_img, ssd, ssd_transform, category_index, lstm, lstm_transform, results, window_size, angle_offsets, camera_output_path, visualized_output_path, fps, device):
                        next_img += 1

                else: # means that I am done analyzing a trial

                    next_camera_output_path = os.path.join(camera_output_base, experiment_name, trial_prefix + str(trial_i + 1))
                    next_trial_exists = os.path.exists(next_camera_output_path)
                    with open(program_json_path, "r") as file:
                        program_dict = json.load(file)
                    experiment_complete = program_dict["experiments"][experiment_i]["completed"]

                    # checking if in the 'waiting period' -> waiting for another trial or for experiment to be marked as completed
                    if not next_trial_exists and not experiment_complete:
                        string += ", waiting"
                        print(string)
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
                    string += "saved trial omr"

                    if next_trial_exists:
                        trial_i += 1
                    # code below will only run if experiment has been completed
                    elif experiment_complete:
                        if experiment_i + 1 >= max_experiments: # means that there are no more experiments
                            program_state = "inactive"
                            program_dict["experiments"] = []
                            with open(program_json_path, "w") as file:
                                json.dump(program_dict, file, indent=4)
                            experiment_i = -1
                            trial_i = -1
                        else: # means that I should progress to the next experiment
                            experiment_i += 1
                            experiment_name = program_dict["experiments"][experiment_i]["name"]
                            trial_i = 0
                            max_img = program_dict["experiments"][experiment_i]["expectedImages"]

                    # setting up for next trial
                    next_img = 0
                    results = torch.zeros(4, max_img)
                    visualized_output_path = os.path.join(visualized_output_base, experiment_name, trial_prefix + str(trial_i))
                    camera_output_path = os.path.join(camera_output_base, experiment_name, trial_prefix + str(trial_i))
            print(string)