from src import transforms
from src.ssd_model import SSD300, Backbone
import torch
import os
import json
from PIL import Image
from src.draw_box_utils import draw_objs
import math
import numpy as np
from datetime import datetime

def find_box_center(box):
    return [0.5 *(box[0] + box[2]), 0.5 * (box[1] + box[3])]

def dot(v1, v2):
    return v1[0] * v2[0] + v1[1] * v2[1]


# these two functions are outdated
def run_ssd_eval(should_display_image):
    visualization_output_folder_path = ("../../liveData/ssdVisualizedImages")
    image_folder_path = "../../liveData/cameraImages"
    json_path = "model/pascal_voc_classes.json"
    model_path = "model/model.pth"

    # loading model
    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")  # using GPU if possible
    print("starting SSD eval on {}".format(device))
    model = SSD300(backbone=Backbone(), num_classes=3)
    weights_dict = torch.load(model_path, map_location='cpu')
    weights_dict = weights_dict["model"] if "model" in weights_dict else weights_dict
    model.load_state_dict(weights_dict)
    model.to(device)

    # loading model inputs
    images = os.listdir(image_folder_path)
    if len(images) == 0:
        raise Exception("on images provided for model to evaluate")

    assert os.path.exists(json_path), "file '{}' dose not exist.".format(json_path)
    json_file = open(json_path, 'r')
    class_dict = json.load(json_file)
    json_file.close()
    category_index = {str(v): str(k) for k, v in class_dict.items()}

    data_transform = transforms.Compose([transforms.Resize(),
                                         transforms.ToTensor(),
                                         transforms.Normalization()])
    results = []

    # running evaluations
    model.eval()
    with torch.no_grad():
        # used to warm up model
        init_img = torch.zeros((1, 3, 300, 300), device=device)
        model(init_img)

        # looping through images
        for index in range(0, len(os.listdir(image_folder_path)) - 1):
            # print("\nindex {} | starting image evaluation".format(index))

            # load current image
            img_Path = os.path.join(image_folder_path, str(index) + '.png')
            original_img = Image.open(img_Path)
            img, _ = data_transform(original_img)
            # expand batch dimension
            img = torch.unsqueeze(img, dim=0)

            predictions = model(img.to(device))[0]  # bboxes_out, labels_out, scores_out
            # predict_boxes is a 2d list; each inner list holds the xmin, ymin, xmax, and ymax for a bounding box, respectively
            predict_boxes = predictions[0].to("cpu").numpy()
            predict_classes = predictions[1].to("cpu").numpy()
            predict_scores = predictions[2].to("cpu").numpy()
            # re-scaling model outputs to normal screen dimensions
            predict_boxes[:, [0, 2]] = predict_boxes[:, [0, 2]] * original_img.size[0]
            predict_boxes[:, [1, 3]] = predict_boxes[:, [1, 3]] * original_img.size[1]

            # analyzing model predictions
            mouse_data = get_head_and_tail_data(predict_boxes, predict_classes, category_index, original_img.size)
            if not mouse_data["ssd_successful"]:
                continue
            # saving data
            results.append([mouse_data['head_angle'], mouse_data['tail_angle']])

            # visualizing image
            if should_display_image:
                plot_img = draw_objs(original_img,
                                     predict_boxes[:, :],
                                     predict_classes[:],
                                     predict_scores[:],
                                     category_index=category_index,
                                     box_thresh=0.5,
                                     line_thickness=3,
                                     font='arial.ttf',
                                     font_size=20,
                                     draw_boxes_on_image=True,
                                     mouse_data=mouse_data)
                output_path = os.path.join(visualization_output_folder_path, str(index) + ".png")
                plot_img.save(output_path)

    print("num results: " + str(len(results)))
    return results

def process_ssd_outputs(ssd_outputs, phase_compensate_threshold, window_size, fps):

    # index 0 - corrected head angles (degrees)
    # index 1 - corrected tail angles (degrees)
    # index 2 - slopes of best fit lines for head angles over all windows (deg/s)
    # index 3 - head angle residuals
    # index 4 - slopes for tail angles (deg/s)
    # index 5 - tail angle residuals
    results = [[], [], [], [], [], []]


    # fixing transition issue from 0 to 2pi of head and tail angles
    for i in range(0, len(ssd_outputs)-1):
        compensate_angle(ssd_outputs, i, 0, phase_compensate_threshold)
        compensate_angle(ssd_outputs, i, 1, phase_compensate_threshold)
    # converting to degrees and storing values in results
    for i in range(len(ssd_outputs)):
        results[0].append(ssd_outputs[i][0] * 180 / math.pi)
        results[1].append(ssd_outputs[i][1] * 180 / math.pi)
    # sliding window approach to obtain slopes of best fit lines and residuals of best fit lines over window
    for i in range(2, len(results[0])+1):
        head_window = results[0][int(max(0, i-window_size)):i]
        p_head, head_residual = get_window_data(head_window, fps)

        results[2].append(float(p_head[0]))
        results[3].append(float(head_residual))

        tail_window = results[1][int(max(0, i-window_size)):i]
        p_tail, tail_residual = get_window_data(tail_window, fps)

        results[4].append(float(p_tail[0]))
        results[5].append(float(tail_residual))

    print('ssd outputs processed')
    return results



def get_head_and_tail_data(numpy_predict_boxes, predict_classes, category_indices, img_size):
    # converting from numpy data type to regular python float
    predict_boxes = []
    for numpy_box in numpy_predict_boxes:
        predict_boxes.append([float(numpy_box[0]), float(numpy_box[1]), float(numpy_box[2]), float(numpy_box[3])])


    # finding ear and tail positions from model
    ear_poses = []
    tail_poses = []
    # print(category_indices)
    # print(predict_classes)
    for i in range(len(predict_boxes)):
        if category_indices[str(predict_classes[i])] == "e":
            ear_poses.append(find_box_center(predict_boxes[i]))
        else:
            tail_poses.append(find_box_center(predict_boxes[i]))

    dict = {
        "ssd_successful": len(ear_poses) == 2 and len(tail_poses) == 1,
        "head_angle": 0,
        "tail_angle": 0
    }
    if not dict["ssd_successful"]:
        return dict
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
        "ears_center": ear_mid,
        "tail_pos": tail_poses[0]
    }


def get_window_data(window, fps):
    avg = mean(window)
    window = [value - avg for value in window]
    x_values = [x / fps for x in range(len(window))]
    p = np.polyfit(x_values, window, 1)
    y_fit = np.polyval(p, x_values)
    residuals = window - y_fit
    normr = np.linalg.norm(residuals)
    return p, normr

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
def analyze_camera_img(img_i, model, data_transform, category_index, results, window_size, angle_offsets, fps, image_folder_path):
    img_path = os.path.join(image_folder_path, str(img_i) + '.png')
    if not os.path.exists(img_path):
        return False
    original_img = Image.open(img_path)

    # running model
    predict_boxes, predict_classes, predict_scores = run_ssd(model, data_transform, original_img)
    # analyzing model predictions
    data = get_head_and_tail_data(predict_boxes, predict_classes, category_index, original_img.size)

    # copying last frames data if ssd fails; if it fails on first frame, then it takes the default values
    if not data["ssd_successful"]:
        if img_i > 0:
            results[0].append(results[0][img_i - 1])
            results[1].append(results[1][img_i - 1])
        else:
            results[0].append(data["head_angle"])
            results[1].append(data["tail_angle"])
        return True

    # converting to degrees
    data["head_angle"] *= 180 / math.pi
    data["tail_angle"] *= 180 / math.pi

    # correcting head and tail angle wraparound issue
    if img_i > 0:
        data["head_angle"] += angle_offsets[0]
        data["tail_angle"] += angle_offsets[1]

        dif = data["head_angle"] - results[0][img_i-1]
        if abs(dif) > angle_offsets[2]:
            offset = -sign(dif) * 360
            angle_offsets[0] += offset
            data["head_angle"] += offset

        dif = data["tail_angle"] - results[1][img_i - 1]
        if abs(dif) > angle_offsets[2]:
            offset = -sign(dif) * 360
            angle_offsets[1] += offset
            data["tail_angle"] += offset

    results[0].append(data["head_angle"])
    results[1].append(data["tail_angle"])

    if img_i == 0:
        return True

    # sliding window approach
    head_window = results[0][int(max(0, img_i + 1 - window_size)):img_i + 1]
    p_head, head_residual = get_window_data(head_window, fps)
    results[2].append(float(p_head[0]))
    results[3].append(float(head_residual))


    tail_window = results[1][int(max(0, img_i + 1 - window_size)):img_i + 1]
    p_tail, tail_residual = get_window_data(tail_window, fps)
    results[4].append(float(p_tail[0]))
    results[5].append(float(tail_residual))

    return True

if __name__ == "__main__":

    # necessary file paths
    program_path = "../../liveData/programInfo.json"
    visualization_output_folder_path = "../../liveData/ssdVisualizedImages"
    image_folder_path = "../../liveData/cameraImages"
    json_path = "model/pascal_voc_classes.json"
    model_path = "model/model.pth"
    result_output_path = "../../liveData/mouseData.json"

    # loading model
    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")  # using GPU if possible
    print("loading SSD on {}".format(device))
    model = SSD300(backbone=Backbone(), num_classes=3)
    weights_dict = torch.load(model_path, map_location='cpu')
    weights_dict = weights_dict["model"] if "model" in weights_dict else weights_dict
    model.load_state_dict(weights_dict)
    model.to(device)
    model.eval()

    # loading model dict
    assert os.path.exists(json_path), "file '{}' dose not exist.".format(json_path)
    json_file = open(json_path, 'r')
    class_dict = json.load(json_file)
    json_file.close()
    category_index = {str(v): str(k) for k, v in class_dict.items()}

    # transformation to normalize images for ssd input
    data_transform = transforms.Compose([transforms.Resize(),
                                         transforms.ToTensor(),
                                         transforms.Normalization()])

    fps = 0
    with open(program_path, "r") as file:
        fps = json.load(file)["fps"]
    spf = 1/fps

    active = True
    next_img = 0 # name of next image that needs to be analyzed
    max_img = 12
    test_time = 10
    rest_time = 10
    start_time = datetime.now()


    window_size = 48
    results = [[], [], [], [], [], []] #  head & tail angles, head slopes, head residuals, tail slopes, tail residuals
    angle_offsets = [0, 0, 5*180/math.pi] # 0 is head offsets, 1 is tail offsets, 2 is threshold to offset

    with torch.no_grad():
        # warming up model
        init_img = torch.zeros((1, 3, 300, 300), device=device)
        model(init_img)

        prev_time = datetime.now()
        while True:
            #print(f"nextI: {next_img}, maxI: {max_img}, active: {active}, testTime: {test_time}, restTime: {rest_time}")
            # ensuring fps matches with program fps
            dif = datetime.now() - prev_time
            if dif.total_seconds() < spf:
                continue
            prev_time = datetime.now()

            # checking for when trial starts
            resting = False
            if active:
                time_since_start = datetime.now() - start_time
                if next_img >= max_img:
                    resting = True
                elif analyze_camera_img(next_img, model, data_transform, category_index, results, window_size, angle_offsets, fps, image_folder_path):
                    next_img += 1
                    with open(result_output_path, "w") as file:
                        json.dump({"head_angle": results[0], "tail_angle": results[1]}, file, indent=4)

            if not active or resting:
                with open(program_path, 'r') as file:
                    dict = json.load(file)
                    if not active and dict["trialActive"]:
                        active = True
                        test_time = dict["testTime"] # i expect this to be in seconds
                        rest_time = dict["restTime"]
                        start_time = datetime.now()
                    elif not dict["trialActive"]:
                        active = False
                        test_time = 0
                        rest_time = 0
