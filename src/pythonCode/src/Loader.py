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
        mean = signal.mean(dim=-1, keepdim=True)  # Mean across features for each channel
        std = signal.std(dim=-1, keepdim=True)    # Std across features for each channel

        # Avoid division by zero
        std[std == 0] = 1

        # Normalize the signal
        return (signal - mean) / std
