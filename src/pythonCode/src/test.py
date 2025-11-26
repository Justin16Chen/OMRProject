import sys

import torch
import torch.backends.cudnn as cudnn
import torch.nn as nn
import os
import Loader
from LSTMClassifier import LSTMClassifier
import torch.optim as optim
import copy
from torch.utils.data import DataLoader
from tqdm import tqdm
import matplotlib.pyplot as plt
from torchmetrics.classification import BinaryAccuracy
from utils import AverageMeter
import numpy as np

# Ensure OpenCV DLLs are discoverable on Windows
if sys.platform == "win32":
    # Replace the path below with your actual Conda environment
    opencv_dll_path = r"C:\Users\justi\anaconda3\envs\omrEnv\Library\bin"
    os.add_dll_directory(opencv_dll_path)

if __name__ == '__main__':
    # for index in range(2, 18):
    #     test_path = r'Data/Data_' + str(index) +'.csv'
    #
    #     batch_size = 1
    #     epochAcc = []
    #     epochValLoss = []
    #
    #     outputs_dir = os.path.join(outputs_dir_base, '{}'.format('BCE_LSTMClassifier_Trail1_Norm'))
    #
    #     cudnn.benchmark = False
    #     device = torch.device('cuda:0' if torch.cuda.is_available() else 'cpu')
    #     model = LSTMClassifier().to(device)
    #     criterion = nn.BCELoss()
    #     accuracy_metric = BinaryAccuracy().to(device)
    #     checkpoint = torch.load(os.path.join(outputs_dir, 'best.pkl'))
    #     model.load_state_dict(checkpoint['model_state_dict'])
    #     eval_dataset = Loader.ReflexDataset(test_path)
    #     eval_dataloader = DataLoader(dataset=eval_dataset, shuffle=False, batch_size=batch_size)
    #
    #     model.eval()
    #     epoch_val_losses = AverageMeter()
    #     acc_val = AverageMeter()
    #     prediction = np.zeros((len(eval_dataloader)+1, 1))
    #     i = 1
    #
    #     for data in eval_dataloader:
    #         inputs, labels = data
    #         inputs = inputs.to(device)
    #         labels = labels.to(device)
    #         labels = labels.to(device).unsqueeze(-1)
    #         with torch.no_grad():
    #             preds = model(inputs)
    #         if preds.cpu().numpy().squeeze() > 0.5:
    #             prediction[i,:] = 1
    #         else:
    #             prediction[i, :] = 0
    #         val_loss = criterion(preds, labels)
    #         accuracy = accuracy_metric(preds, labels)
    #         epoch_val_losses.update(val_loss.item(), len(inputs))
    #         acc_val.update(accuracy.item(), len(inputs))
    #         i = i + 1
    #     mat_data = {"array_name": prediction}
    #     savemat(test_path.replace('.csv','.mat'), mat_data)
    #     epochValLoss.append(epoch_val_losses.avg)
    #     epochAcc.append(acc_val.avg)
    #     print('eval loss: {:.6f}, Accuracy={:.4f}'.format(epoch_val_losses.avg, acc_val.avg))
    test_path = r"C:\Users\justi\Documents\GitHub\OMRProject\src\pythonCode\model\Data_shuffled_train.csv"

    batch_size = 1
    epochAcc = []
    epochValLoss = []

    outputs_dir = r"C:\Users\justi\Documents\omr images\raw images\csvOutput"

    cudnn.benchmark = False
    device = torch.device('cuda:0' if torch.cuda.is_available() else 'cpu')
    model = LSTMClassifier().to(device)
    criterion = nn.BCELoss()
    accuracy_metric = BinaryAccuracy().to(device)
    checkpoint = torch.load(r"C:\Users\justi\Documents\GitHub\OMRProject\src\pythonCode\model\best.pkl", map_location=device)
    model.load_state_dict(checkpoint['model_state_dict'])
    eval_dataset = Loader.ReflexDataset(test_path)
    eval_dataloader = DataLoader(dataset=eval_dataset, shuffle=False, batch_size=batch_size)


    model.eval()
    epoch_val_losses = AverageMeter()
    acc_val = AverageMeter()
    prediction = np.zeros((len(eval_dataloader)+1, 1))
    i = 1

    max_i = 100

    for data in eval_dataloader:
        if i > max_i:
            break;
        print("i: " + str(i))
        inputs, labels = data
        inputs = inputs.to(device)
        labels = labels.to(device)
        labels = labels.to(device).unsqueeze(-1)
        with torch.no_grad():
            preds = model(inputs)
        if preds.cpu().numpy().squeeze() > 0.5:
            prediction[i,:] = 1
        else:
            prediction[i, :] = 0
        print(prediction[i, :],'with label', labels)
        val_loss = criterion(preds, labels)
        accuracy = accuracy_metric(preds, labels)
        epoch_val_losses.update(val_loss.item(), len(inputs))
        acc_val.update(accuracy.item(), len(inputs))
        i = i + 1
    # mat_data = {"array_name": prediction}
    print("predictions", str(prediction))
    # savemat(test_path.replace('.csv','.mat'), mat_data)
    epochValLoss.append(epoch_val_losses.avg)
    epochAcc.append(acc_val.avg)
    print('eval loss: {:.6f}, Accuracy={:.4f}'.format(epoch_val_losses.avg, acc_val.avg))



