package org.example.cameraCode;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.*;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.example.trialControlPanel.parentClasses.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
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

    public enum State {
        READY,
        WAITING,
        IN_PROCESS,
        STOPPED_EARLY,
        PERMANENTLY_STOPPED
    }
    private VideoCapture cap;
    private final Mat latestImage;
    private final ArrayList<Mat> images;
    private volatile int cameraIndex, sendIndex;
    private int imageIndexCap;
    private final ScheduledExecutorService imageSendExecutor;
    private final ExecutorService imageSaveExecutor;
    private ScheduledFuture<?> imageSendHandler;
    private volatile State sendState, saveState;
    private boolean connected, recording;
    private boolean saveImage;
    private int devicePort;
    private final Core core;

    public CameraManager(int devicePort, Core core) {
        this.devicePort = devicePort;
        this.core = core;

        latestImage = new Mat();
        images = new ArrayList<>();
        saveImage = true;
        cameraIndex = 0;
        sendIndex = 0;
        recording = false;
        sendState = State.READY;
        saveState = State.WAITING;

        cap = new VideoCapture(devicePort);
        connected = cap.isOpened();

        imageSendExecutor = Executors.newSingleThreadScheduledExecutor();
        imageSaveExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public int getFrameWidth() {
        return (int)cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
    }
    public int getFrameHeight() {
        return (int)cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
    }

    // sending images
    public void startSendingImages(long interval) {
        if (imageSendHandler == null || imageSendHandler.isCancelled()) {
            sendState = State.IN_PROCESS;
            saveState = State.WAITING;
            imageSendHandler = imageSendExecutor.scheduleAtFixedRate(this::updateImageSending, 0, interval, TimeUnit.NANOSECONDS);
        }
        else
            throw new IllegalStateException("cannot start sending data when it is already started sending");
    }
    public void stopSendingImages() {
        if (imageSendHandler != null && !imageSendHandler.isCancelled())
            imageSendHandler.cancel(true);
        if (sendIndex <= imageIndexCap)
            sendState = State.STOPPED_EARLY;
        else
            sendState = State.READY;
        sendIndex = 0;
    }
    public void shutDownExecutors() {
        sendState = State.PERMANENTLY_STOPPED;
        saveState = State.PERMANENTLY_STOPPED;
        imageSendExecutor.shutdown();
        imageSaveExecutor.shutdown();
    }

    // other stuff
    public int getImageIndex() {
        return cameraIndex;
    }
    public int getSendIndex() { return sendIndex; }
    public State getSendState() {
        return sendState;
    }
    public State getSaveState() {
        return saveState;
    }
    public void saveImageData(String folder) {
        saveState = State.IN_PROCESS;
        imageSaveExecutor.submit(() -> {
            System.out.println("starting image save executor");
            long startTime = System.nanoTime();
            for (int i = 0; i < images.size(); i++) {
                Imgcodecs.imwrite(Path.of(folder, i + ".png").toString(), images.get(i));
            }
            saveState = State.READY;
            System.out.println("saved images to folder: " + folder);
            System.out.println("time to save " + images.size() + " images: " + (System.nanoTime() - startTime) / 1_000_000L);
            System.out.println("image index cap: " + imageIndexCap);
        });
    }
    public void clearOldImageData() {
        if (sendState == State.IN_PROCESS)
            throw new IllegalStateException("cannot reset image index in CameraManager when it has not finished SENDING old images");
        if (saveState == State.IN_PROCESS)
            throw new IllegalStateException("cannot reset image index in CameraManager when it has not finished SAVING old images");

        cameraIndex = 0;
        sendIndex = 0;
        images.clear();
    }
    public int getImageIndexCap() {
        return imageIndexCap;
    }
    public void setImageIndexCap(int maxIndex) {
        this.imageIndexCap = maxIndex;
    }
    public void fillImagesToCap() {
        int failedAttempts = 0;
        while (cameraIndex <= imageIndexCap && failedAttempts < 10) {
            if (trySendImage())
                cameraIndex++;
            else
                failedAttempts++;
        }
    }
    public void trySetDevicePort(int index) {
        cap = new VideoCapture(index);
        devicePort = index;
        connected = cap.isOpened();
    }
    public void updateImageReading() {
        if (!recording || !connected)
            return;

        cap.read(latestImage);
        cameraIndex++;

        // turning off camera once enough images are saved
        if(saveImage) {
            images.add(latestImage);
            if (cameraIndex > imageIndexCap && imageIndexCap > 0)
                saveImage = false;
        }
    }
    private void updateImageSending() {
        if(sendIndex < images.size()) {
            if (trySendImage()) {
                sendIndex++;
            }
        }
        if (sendIndex > imageIndexCap) {
            sendState = State.READY;
            imageSendHandler.cancel(true);
        }
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
    public void setSaveImage(boolean saveImage) {
        this.saveImage = saveImage;
    }

    private boolean trySendImage() {
        Mat img = images.get(sendIndex);
        if(img.empty())
            return false;
        try {
//            double before = System.currentTimeMillis();

            byte[] indexBytes = ByteBuffer.allocate(4).putInt(cameraIndex).array();

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

    public Image getLatestImage() {
        if (latestImage.empty())
            return null;
        return matToImage(latestImage);
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
