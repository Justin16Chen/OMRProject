package org.example.cameraCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class CameraManager {
    static {
        // loading openCV
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("local.properties"));
            String dllPath = props.getProperty("opencv.dll.path");
            if(dllPath == null)
                throw new RuntimeException("Missing opencv.dll.path in local.properties");
            System.load(dllPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load local.properties", e);
        }
        System.out.println("successfully loaded opencv dll");
    }

    public static final String RAW_IMAGES_PATH = "cameraImages";

    private VideoCapture cap;
    private final Mat image;
    private int i;
    private boolean connected, recording, savePermanentImages;
    private int devicePort;

    public CameraManager(int devicePort) {
        this.devicePort = devicePort;

        image = new Mat();
        savePermanentImages = true;
        i = 0;
        recording = false;

        cap = new VideoCapture(devicePort);
        connected = cap.isOpened();
    }
    public void trySetDevicePort(int index) {
        cap = new VideoCapture(index);
        devicePort = index;
        connected = cap.isOpened();
    }
    public void update() {
        if (!recording || !connected)
            return;
        if(savePermanentImages) {
            if (trySaveImage())
                i++;
        }
        else
            cap.read(image);
    }

    public int getDevicePort() {
        return devicePort;
    }
    public boolean isConnected() {
        return connected;
    }
    public void startRecording() {
        recording = true;
    }
    public void stopRecording() {
        recording = false;
    }
    public void setSavePermanentImages(boolean savePermanent) {
        this.savePermanentImages = savePermanent;
    }

    private boolean trySaveImage() {
        if (!connected)
            return false;
        if(cap.read(image)) {
            if(image.empty())
                return false;
            Imgcodecs.imwrite(RAW_IMAGES_PATH + "\\" + i + ".png", image);
            return true;
        }
        return false;
    }

    public void clearFolder() {
        File folder = new File(RAW_IMAGES_PATH);
        File[] files = folder.listFiles();
        if (files != null)
            for (File file : files)
                file.delete();
    }

    public Image getLatestImage() {
        return matToImage(image);
    }

    private Image matToImage(Mat mat) {
        try {
            // Convert to BGR if needed
            if (mat.channels() == 1) {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2BGR);
            } else if (mat.channels() == 4) {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2BGR);
            }

            int width = mat.width();
            int height = mat.height();
            byte[] data = new byte[width * height * (int)mat.elemSize()];
            mat.get(0, 0, data);

            WritableImage image = new WritableImage(width, height);
            PixelWriter pw = image.getPixelWriter();

            pw.setPixels(0, 0, width, height,
                    PixelFormat.getByteBgraInstance(),
                    convertBGRtoBGRA(data, width, height),
                    0, width * 4);

            return image;
        } catch (Exception e) {
            System.err.println("Failed to convert Mat to Image: " + e);
            return null;
        }
    }

    private byte[] convertBGRtoBGRA(byte[] bgr, int width, int height) {
        byte[] bgra = new byte[width * height * 4];
        for (int i = 0, j = 0; i < bgr.length; i += 3, j += 4) {
            bgra[j] = bgr[i];       // B
            bgra[j + 1] = bgr[i + 1]; // G
            bgra[j + 2] = bgr[i + 2]; // R
            bgra[j + 3] = (byte)255;  // A (opaque)
        }
        return bgra;
    }
}
