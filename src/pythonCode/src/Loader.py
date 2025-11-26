import pandas as pd
import torch
from torch.utils.data import Dataset, DataLoader

class ChannelNormalize:
    def __call__(self, signal):
        """
        Normalize each channel of the signal to have mean 0 and std 1.

        Args:
            signal (torch.Tensor): Input signal of shape (n_features, n_channels).

        Returns:
            torch.Tensor: Normalized signal of the same shape.
        """
        # Compute mean and std for each channel (dimension -2)
        mean = signal.mean(dim=-2, keepdim=True)  # Mean across features for each channel
        std = signal.std(dim=-2, keepdim=True)    # Std across features for each channel

        # Avoid division by zero
        std[std == 0] = 1

        # Normalize the signal
        return (signal - mean) / std


# Define the custom Dataset class
class ReflexDataset(Dataset):
    def __init__(self, csv_file, transform=ChannelNormalize()):
        """
        Args:
            csv_file (str): Path to the CSV file.
            transform (callable, optional): Optional transform to be applied
                on a signal.
        """
        self.data = pd.read_csv(csv_file)
        self.transform = transform

        # Split data into features (signals) and labels
        self.signals1 = self.data.iloc[:, 1:49].values  # All columns except the first one
        self.signals2 = self.data.iloc[:, 49:].values  # All columns except the first one
        self.labels = self.data.iloc[:, 0].values   # The first column

    def __len__(self):
        """Returns the total number of samples"""
        return len(self.data)

    def __getitem__(self, idx):
        """
        Args:
            idx (int): Index of the sample to fetch.

        Returns:
            tuple: (signal, label) where signal is a 1x48 vector and label is its corresponding label.
        """
        signal1 = torch.tensor(self.signals1[idx], dtype=torch.float32)
        signal2 = torch.tensor(self.signals2[idx], dtype=torch.float32)
        signal1 = signal1.unsqueeze(-1)  # Shape: (3, 1)
        signal2 = signal2.unsqueeze(-1)  # Shape: (3, 1)
        signal = torch.cat([signal1, signal2], dim=-1)
        label = torch.tensor(self.labels[idx], dtype=torch.float32)  # Change dtype if labels are not integers

        # Apply any optional transformation
        if self.transform:
            signal = self.transform(signal)
        return signal, label


if __name__ == '__main__':
    csv_file = r'Data/Data_1.csv'  # Replace with your actual file path
    dataset = ReflexDataset(csv_file)

    # Create DataLoader for batching and shuffling
    train_loader = DataLoader(dataset, batch_size=1, shuffle=False)

    # Iterate through the data
    i=1
    for batch in train_loader:
        signals, labels = batch
        i = i+1
        if i == 155:
            print(signals)
            print(labels)

        # print("Signals batch shape:", signals.shape)  # Should be (batch_size, sequence_length, input_size)
        # print("Labels batch shape:", labels.shape)    # Should be

        # break

