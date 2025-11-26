package org.example.trialControlPanel.omrResults;

import org.opencv.core.Mat;

import java.awt.image.RenderedImage;
import java.util.ArrayList;

public record TrialResult (ArrayList<Mat> rawImages, ArrayList<RenderedImage> visualizedImages, double[] results, ArrayList<Double> timestamps, ArrayList<Double> headAngles, ArrayList<Double> tailAngles) {}
