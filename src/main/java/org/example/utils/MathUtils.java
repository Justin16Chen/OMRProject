package org.example.utils;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.opencv.core.Mat;
import javafx.scene.image.Image;

import java.text.DecimalFormat;

// helps for concise, easy printing to telemetry
public class MathUtils {
    public static double correctRad(double rad) {
        while (rad < 0)
            rad += 2 * Math.PI;
        while (rad >= 2 * Math.PI)
            rad -= 2 * Math.PI;
        return rad;
    }
    public static double correctDeg(double deg) {
        while (deg < 0)
            deg += 360;
        while (deg >= 360)
            deg -= 360;
        return deg;
    }
    public static String format2(Number num) {
        return format(num, 2);
    }
    public static String format3(Number num) { return format(num, 3); }
    public static String format(Number num, int decimalPlaces) {
        StringBuilder decimals = new StringBuilder();
        for (int i=0; i<decimalPlaces; i++)
            decimals.append("#");
        DecimalFormat customDf = new DecimalFormat("#." + decimals);
        return customDf.format(num);
    }
    public static String format2(double[] nums) {
        StringBuilder total = new StringBuilder();
        for (double num : nums)
            total.append(format2(num)).append(", ");
        return total.substring(0, total.length() - 2);
    }
    public static String format3(double[] nums) {
        StringBuilder total = new StringBuilder();
        for (double num : nums)
            total.append(format3(num)).append(", ");
        return total.substring(0, total.length() - 2);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static Image matToImage(Mat frame) {
        try {
            return matToWritableImage(frame);
        } catch (Exception e) {
            System.err.println("Cannot convert Mat to Image: " + e);
            return null;
        }
    }

    private static WritableImage matToWritableImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        int channels = mat.channels();

        byte[] sourcePixels = new byte[width * height * channels];
        mat.get(0, 0, sourcePixels);

        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();

        if (mat.channels() == 3) {
            // OpenCV uses BGR, JavaFX uses RGB → convert
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = (y * width + x) * 3;

                    int b = sourcePixels[index] & 0xFF;
                    int g = sourcePixels[index + 1] & 0xFF;
                    int r = sourcePixels[index + 2] & 0xFF;

                    int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                    pw.setArgb(x, y, argb);
                }
            }
        } else if (mat.channels() == 1) {
            // Grayscale Mat
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int gray = sourcePixels[y * width + x] & 0xFF;
                    int argb = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
                    pw.setArgb(x, y, argb);
                }
            }
        }

        return image;
    }

}
