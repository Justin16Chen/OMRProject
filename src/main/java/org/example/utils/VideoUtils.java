package org.example.utils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoWriter;

import java.awt.image.*;
import java.util.ArrayList;

public class VideoUtils {

    /** Converts an ArrayList of Mats to a video file */
    public static void matsToVideo(ArrayList<Mat> mats, String filePath, double fps) {
        if (mats == null || mats.isEmpty()) {
            System.out.println("No frames to write!");
            return;
        }

        Size frameSize = new Size(mats.get(0).cols(), mats.get(0).rows());
        int fourcc = VideoWriter.fourcc('X','2','6','4');
        VideoWriter writer = new VideoWriter(filePath, fourcc, fps, frameSize, true);

        if (!writer.isOpened()) {
            System.out.println("Failed to open video writer!");
            return;
        }

        for (Mat frame : mats) {
            writer.write(frame);
        }

        writer.release();
        System.out.println("Video saved to: " + filePath);
    }

    /** Converts an ArrayList of RenderedImages to a video file */
    public static void renderedImagesToVideo(ArrayList<RenderedImage> images, String filePath, double fps) {
        if (images == null || images.isEmpty()) {
            System.out.println("No images to write!");
            return;
        }

        int width = images.get(0).getWidth();
        int height = images.get(0).getHeight();
        Size frameSize = new Size(width, height);

        int fourcc = VideoWriter.fourcc('X','2','6','4'); // Change codec if needed
        VideoWriter writer = new VideoWriter(filePath, fourcc, fps, frameSize, true);

        if (!writer.isOpened()) {
            System.out.println("Failed to open video writer!");
            return;
        }

        for (RenderedImage img : images) {
            BufferedImage bImg = toBufferedImage(img);
            Mat mat = bufferedImageToMatBGR(bImg);
            writer.write(mat);
        }

        writer.release();
        System.out.println("Video saved to: " + filePath);
    }

    /** Converts RenderedImage to BufferedImage */
    private static BufferedImage toBufferedImage(RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        } else {
            BufferedImage bImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Raster raster = img.getData();
            bImg.setData(raster);
            return bImg;
        }
    }

    /** Converts BufferedImage (assumed BGR) to Mat without channel swapping */
    private static Mat bufferedImageToMatBGR(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        Mat mat = new Mat(height, width, CvType.CV_8UC3);

        int[] pixels = new int[width * height];
        bi.getRGB(0, 0, width, height, pixels, 0, width); // returns ARGB

        byte[] matData = new byte[width * height * 3];
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            byte b = (byte) (argb & 0xFF);
            byte g = (byte) ((argb >> 8) & 0xFF);
            byte r = (byte) ((argb >> 16) & 0xFF);

            // Since your image is BGR, write directly
            matData[i * 3]     = b;
            matData[i * 3 + 1] = g;
            matData[i * 3 + 2] = r;
        }

        mat.put(0, 0, matData);
        return mat;
    }
}
