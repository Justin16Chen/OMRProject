package org.example.cameraCode;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.example.trialControlPanel.parentClasses.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.Videoio;

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

    public enum SaveState {
        READY,
        WAITING,
        IN_PROGRESS,
        PERMANENTLY_STOPPED
    }
    public enum SendState {
        READY,
        READY_IMPERFECTLY, // send all possible images on camera, but still haven't met imageIndexCap requirement b/c camera simply did not record enough images
        IN_PROGRESS,
        STOPPED_EARLY,
        PERMANENTLY_STOPPED
    }

    private final CameraImageGrabber cameraImageGrabber;
    private final ArrayList<Mat> images;
    public final ArrayList<Double> timeStampsMs;
    private final AtomicInteger numCameraImagesSaved, sendIndex;
    private int maxImageIndex; // index of last saved image - num images to save - maxImageIndex + 1
    private volatile SaveState saveState;
    private volatile SendState sendState;
    private boolean connected;
    private boolean readImagesFromCamera;
    private int devicePort;
    private final Core core;
    public VisualizedImageReader visualizedImageReader;

    public CameraManager(int devicePort, Core core) {
        this.devicePort = devicePort;
        this.core = core;

        images = new ArrayList<>();
        timeStampsMs = new ArrayList<>();
        readImagesFromCamera = true;
        numCameraImagesSaved = new AtomicInteger(0);
        sendIndex = new AtomicInteger(0);
        sendState = SendState.READY;
        saveState = SaveState.READY;

        cameraImageGrabber = new CameraImageGrabber(devicePort);
        connected = cameraImageGrabber.isConnected();
    }

    public int getFrameWidth() {
        return (int) cameraImageGrabber.cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
    }
    public int getFrameHeight() {
        return (int) cameraImageGrabber.cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
    }

    public void stopEverything() {
        readImagesFromCamera = false;
        sendState = SendState.PERMANENTLY_STOPPED;
        saveState = SaveState.PERMANENTLY_STOPPED;
    }
    public int getSendIndex() { return sendIndex.get(); }
    public SendState getSendState() {
        return sendState;
    }
    public SaveState getSaveState() {
        return saveState;
    }
    public void saveImageData(String folder) {
        saveState = SaveState.IN_PROGRESS;
        new Thread(() -> {
            for (int i = 0; i < images.size(); i++) {
                Imgcodecs.imwrite(Path.of(folder, i + ".png").toString(), images.get(i));
            }
            saveState = SaveState.READY;
            Platform.runLater(core.getStartMenuController()::updateButtonsEnabled);
        }, "save image thread").start();
    }
    public void clearOldImageData() {
        if (sendState == SendState.IN_PROGRESS)
            throw new IllegalStateException("cannot reset image index in CameraManager when it has not finished SENDING old images");
        if (saveState == SaveState.IN_PROGRESS)
            throw new IllegalStateException("cannot reset image index in CameraManager when it has not finished SAVING old images");

        numCameraImagesSaved.set(0);
        sendIndex.set(0);
        images.clear();
        timeStampsMs.clear();
    }
    public int getMaxImageIndex() {
        return maxImageIndex;
    }
    public void setMaxImageIndex(int maxIndex) {
        this.maxImageIndex = maxIndex;
    }
    public void trySetDevicePort(int devicePort) {
        this.devicePort = devicePort;
        cameraImageGrabber.attemptReconnect(devicePort);
        connected = cameraImageGrabber.isConnected();
    }

    public int getDevicePort() {
        return devicePort;
    }
    public boolean isConnected() {
        return connected;
    }
    public boolean isReadingImagesFromCamera() {
        return readImagesFromCamera;
    }
    public void setReadImagesFromCamera(boolean readImagesFromCamera) {
        this.readImagesFromCamera = readImagesFromCamera;
    }

    // start grabbing images on a separate thread and read and store the images at a fixed interval
    public void startReadingImagesFromCamera(long updateTimeNano) {
        readImagesFromCamera = true;
        Thread updateThread = new Thread(() -> {
            long nextFrameTime = System.nanoTime();
            numCameraImagesSaved.set(0);

            while (readImagesFromCamera) {

                updateImageReadingFromCamera();

                nextFrameTime += updateTimeNano;
                long sleepNanos = nextFrameTime - System.nanoTime();

                if (sleepNanos > 0) {
                    try {
                        TimeUnit.NANOSECONDS.sleep(sleepNanos);
                    } catch (InterruptedException ignored) {}
                }

                if (images.size() >= maxImageIndex + 1)
                    readImagesFromCamera = false;
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    // loads the images from the camera to the images list once
    public void updateImageReadingFromCamera() {
        if (!connected)
            return;
//        System.out.print(numCameraImagesSaved.get() + " updating image reading from camera ");

        Mat latestFrame = cameraImageGrabber.getLatestFrame();
        if (!readImagesFromCamera)
            System.out.println("READ IMAGE = FALSE");
        if (latestFrame == null)
            System.out.println("LATEST FRAME IS NULL");

        // saving images
        if(readImagesFromCamera && latestFrame != null) {
            numCameraImagesSaved.set(numCameraImagesSaved.get() + 1);
            images.add(latestFrame.clone());
            timeStampsMs.add(System.nanoTime() * 1.e-6);
            if (numCameraImagesSaved.get() > maxImageIndex + 1 && maxImageIndex > 0) {
                // turn off camera once enough images are saved
                readImagesFromCamera = false;
                cameraImageGrabber.stopGrabbing();
            }
        }
    }
    // sending images to python program to be analyzed as trial is running
    public void startSendingImagesToSSD() {
        sendState = SendState.IN_PROGRESS;
        saveState = SaveState.WAITING;
        new Thread(() -> {
            while (sendState == SendState.IN_PROGRESS) {
                updateImageSending();
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
//            Platform.runLater(core.getStartMenuController()::updateButtonsEnabled);
        }, "send images to SSD thread").start();
    }
    public void stopSendingImages() {
        if (sendIndex.get() <= maxImageIndex)
            sendState = SendState.STOPPED_EARLY;
        else
            sendState = SendState.READY;
        sendIndex.set(0);
    }
    // sends the currently stored images in the images list to the SSD for it to analyze
    private void updateImageSending() {
        if(sendIndex.get() < images.size()) {
            if (trySendImage()) {
                sendIndex.set(sendIndex.get() + 1);
            }
        }
        if (sendIndex.get() >= maxImageIndex + 1) // finished sending all
            sendState = SendState.READY;
        else if (sendIndex.get() >= images.size() && !readImagesFromCamera) // also finished sending, but images are incomplete
            sendState = SendState.READY_IMPERFECTLY;
//        System.out.println("send state: " + getSendState() + " | save state: " + getSaveState() + " | visualized save state: " + visualizedImageReader.getState() + " | send idx: " + getSendIndex() + " | num images in list: " + images.size() + "/" + (maxImageIndex + 1));
    }

    // sends the current image to the SSD through sockets
    private boolean trySendImage() {
        Mat img = images.get(sendIndex.get());
        if(img.empty())
            return false;
        try {
//            double before = System.currentTimeMillis();

            byte[] indexBytes = ByteBuffer.allocate(4).putInt(numCameraImagesSaved.get()).array();

            byte[] imgBytes = new byte[getFrameWidth() * getFrameHeight() * 3];
            img.get(0, 0, imgBytes);
            core.getSocketManager().writeData(indexBytes);
            core.getSocketManager().writeData(imgBytes);
            core.getSocketManager().flush();
            //System.out.println("camera data sent in " + (System.currentTimeMillis() - before) +"ms");
            return true;
        }
        catch (IOException e) {
            System.out.println("failed to send images");
            return false;
        }
    }

    public Image getLatestImageFromGrabber() {
        if (cameraImageGrabber.getLatestFrame() == null || cameraImageGrabber.getLatestFrame().empty())
            return null;
        return matToImage(cameraImageGrabber.getLatestFrame());
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

    // if this is true, the program can start saving the analyzed images from the SSD to the file system
    public boolean finishedSendingImagesToSSD() {
        return sendState == SendState.READY || sendState == SendState.READY_IMPERFECTLY;
    }
    public CameraImageGrabber getImageGrabber() {
        return cameraImageGrabber;
    }
}
