from src import transforms
from src.ssd_model import SSD300, Backbone
import torch
import os
import json
from PIL import Image
from src.draw_box_utils import draw_objs
import math
import numpy as np

def find_box_center(box):
    return [0.5 *(box[0] + box[2]), 0.5 * (box[1] + box[3])]


def dot(v1, v2):
    return v1[0] * v2[0] + v1[1] * v2[1]


def run_ssd_eval(should_display_image):
    image_folder_path = "../../images/cameraImages"
    visualization_output_folder_path = ("../../images/ssdVisualizedImages")
    json_path = "model/pascal_voc_classes.json"
    model_path = "model/model.pth"

    # loading model
    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")  # using GPU if possible
    print("starting model eval on {}".format(device))
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
            mouse_data = analyze_ssd_eval(predict_boxes, predict_classes, category_index, original_img.size)
            if mouse_data == -1:
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


def analyze_ssd_eval(numpy_predict_boxes, predict_classes, category_indices, img_size):
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

    if len(ear_poses) < 2:
        print("WRONG NUM OF EARS: " + str(len(ear_poses)))
        return -1
    elif len(tail_poses) < 1:
        print("WRONG NUM OF TAILS: " + str(len(tail_poses)))
        return -1

    # calculating head direction based on ear and tail positions
    ear_vec = [ear_poses[1][0] - ear_poses[0][0], ear_poses[1][1] - ear_poses[0][1]]
    head_vec = [ear_vec[1], -ear_vec[0]]
    ear_mid = find_box_center([*ear_poses[0], *ear_poses[1]])
    rel_tail_vec = [tail_poses[0][0] - ear_mid[0], tail_poses[0][1] - ear_mid[1]]
    if dot(rel_tail_vec, head_vec) > 0:
        head_vec = [head_vec[0] * -1, head_vec[1] * -1]
    tail_vec = [tail_poses[0][0] - img_size[0]*0.5, tail_poses[0][1] - img_size[1]*0.5]

    return {
        "head_angle": math.atan2(head_vec[1], head_vec[0]),
        "tail_angle": math.atan2(tail_vec[1], tail_vec[0]),
        "ear_poses": ear_poses,
        "ears_center": ear_mid,
        "tail_pos": tail_poses[0]
    }

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



if __name__ == "__main__":
    ssd_output = run_ssd_eval(should_display_image=True)
    lstm_inputs = process_ssd_outputs(ssd_output,5,48,24)
    # run_lstm_eval(lstm_inputs)