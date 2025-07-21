package org.example.cameraCode;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
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

    public static final String rawImagesPath = "cameraImages";

    private final VideoCapture cap;
    private final Mat image;
    private int i;
    private boolean recording;

    public CameraManager() {
        image = new Mat();
        cap = new VideoCapture(0);
        if(!cap.isOpened()) {
            System.out.println("cannot open camera, exiting");
            return;
        }
        i = 0;
        recording = false;
    }
    public void update() {
        if(!recording)
            return;

        if(trySaveImage())
            i++;
    }

    public void setRecording(boolean isRecording) {
        recording = isRecording;
    }

    private boolean trySaveImage() {
        if(cap.read(image)) {
            if(image.empty())
                return false;
            Imgcodecs.imwrite(rawImagesPath + "\\" + i + ".png", image);
            return true;
        }
        return false;
    }
}
