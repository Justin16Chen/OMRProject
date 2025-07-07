package org.example.cameraCode;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.scene.Camera;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class CameraManager {
    static {
        // loading openCV
        System.load("C:\\Users\\czhao\\Documents\\opencv\\build\\java\\x64\\opencv_java4110.dll");
    }
    public static final int cameraFPS = 30;
    public static final String rawImagesPath = "C:\\Users\\czhao\\Documents\\omrProject\\src\\main\\resources\\rawCameraImages";
    private final VideoCapture cap;
    private final Mat image;
    private int i;

    public CameraManager() {
        System.out.println("top of construct");

        cap = new VideoCapture(0);
        image = new Mat();
        i = 0;
        System.out.println(trySaveImage());
        System.out.println("bottom of construct");
    }
    private void clearPrevImages() {
        Path dir = Paths.get(rawImagesPath);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    Files.delete(entry);
                }
            }
            System.out.println("Previous camera image files deleted");
        } catch (IOException e) {
            e.printStackTrace();
        }
        i = 0;
    }

    public void start() {
        // overriding run method
        Thread cameraThread = new Thread(() -> {
            double time = System.currentTimeMillis();
            double lastUpdateTime = time;
            double millisPerFrame = 1000.0 / cameraFPS;
            while (true) {
                time = System.currentTimeMillis();
                if(time - lastUpdateTime > millisPerFrame) {
                    this.update();
                    lastUpdateTime = time;
                }
            }
        });
        cameraThread.start();
    }
    private void update() {
        if(trySaveImage())
            i++;
    }

    private boolean trySaveImage() {
        if(cap.read(image)) {
            Imgcodecs.imwrite(rawImagesPath + "\\" + i + ".png", image);
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        CameraManager cm = new CameraManager();
//        cm.start();
    }
}
