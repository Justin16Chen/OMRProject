import torch
import torch.nn as nn


# class LSTMClassifier(nn.Module):
#     def __init__(self, input_size=2, hidden_size=16, num_layers=16, num_classes=2):
#         super(LSTMClassifier, self).__init__()
#         self.lstm = nn.LSTM(input_size=input_size, hidden_size=hidden_size, num_layers=num_layers, batch_first=True)
#         self.fc = nn.Linear(hidden_size, num_classes)  # Fully connected layer for classification
#
#
#     def forward(self, x):
#         # x: (batch_size, sequence_length, input_size)
#         out, (hn, cn) = self.lstm(x)  # out: (batch_size, sequence_length, hidden_size)
#         out = out[:, -1, :]  # Take the hidden state at the last time step
#         out = self.fc(out) # Fully connected layer
#         return out


class LSTMClassifier(nn.Module):
    def __init__(self, input_size=2, hidden_size=64, num_layers=4, output_size=1):
        super(LSTMClassifier, self).__init__()
        self.lstm = nn.LSTM(input_size=input_size, hidden_size=hidden_size, num_layers=num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_size, output_size)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        # x: (batch_size, sequence_length, input_size)
        lstm_out, (hn, cn) = self.lstm(x)  # lstm_out: (batch_size, sequence_length, hidden_size)
        last_hidden = lstm_out[:, -1, :]   # Get the output of the last time step
        out = self.fc(last_hidden)        # Fully connected layer
        out = self.sigmoid(out)           # Sigmoid activation
        return out