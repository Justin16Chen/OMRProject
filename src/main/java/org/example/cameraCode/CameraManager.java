package org.example.cameraCode;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

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
        STOPPED_EARLY,
        PERMANENTLY_STOPPED
    }
    public enum SendState {
        READY,
        READY_IMPERFECTLY, // send all possible images on camera, but still haven't met imageIndexCap requirement b/c camera simply did not record enough images
        WAITING,
        IN_PROGRESS,
        STOPPED_EARLY,
        PERMANENTLY_STOPPED
    }

    private final CameraImageGrabber cameraImageGrabber;
    private final ArrayList<Mat> images;
    private final AtomicInteger numCameraImagesSaved, sendIndex;
    private int imageIndexCap;
    private final ScheduledExecutorService imageSendExecutor;
    private final ExecutorService imageSaveExecutor;
    private ScheduledFuture<?> imageSendHandler;
    private volatile SaveState saveState;
    private volatile SendState sendState;
    private boolean connected, recording;
    private boolean saveImage;
    private int devicePort;
    private final Core core;
    private double totalTimeToReadImageFromCamera = 0;
    private double totalTimeSleepingBetweenCameraReads, maxSleepTime;
    private int numTimesSleptBetweenCameraReads, numTimesUpdatedCameraRead, numTimesActuallyAddedImage;

    public CameraManager(int devicePort, Core core) {
        this.devicePort = devicePort;
        this.core = core;

        images = new ArrayList<>();
        saveImage = true;
        numCameraImagesSaved = new AtomicInteger(0);
        sendIndex = new AtomicInteger(0);
        recording = false;
        sendState = SendState.READY;
        saveState = SaveState.WAITING;

        cameraImageGrabber = new CameraImageGrabber(devicePort);
        connected = cameraImageGrabber.isConnected();

        imageSendExecutor = Executors.newSingleThreadScheduledExecutor();
        imageSaveExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public int getFrameWidth() {
        return (int) cameraImageGrabber.cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
    }
    public int getFrameHeight() {
        return (int) cameraImageGrabber.cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
    }

    // sending images to python program to be analyzed as trial is running
    public void startSendingImagesToSSD(long interval) {
        if (imageSendHandler == null || imageSendHandler.isCancelled()) {
            sendState = SendState.IN_PROGRESS;
            saveState = SaveState.WAITING;
            imageSendHandler = imageSendExecutor.scheduleAtFixedRate(this::updateImageSending, 0, interval, TimeUnit.NANOSECONDS);
        }
        else
            throw new IllegalStateException("cannot start sending data when it is already started sending");
    }
    public void stopSendingImages() {
        if (imageSendHandler != null && !imageSendHandler.isCancelled())
            imageSendHandler.cancel(true);
        if (sendIndex.get() <= imageIndexCap)
            sendState = SendState.STOPPED_EARLY;
        else
            sendState = SendState.READY;
        sendIndex.set(0);
    }
    public void shutDownExecutors() {
        sendState = SendState.PERMANENTLY_STOPPED;
        saveState = SaveState.PERMANENTLY_STOPPED;
        imageSendExecutor.shutdown();
        imageSaveExecutor.shutdown();
    }

    // other stuff
    public int getNumCameraImagesSaved() {
        return numCameraImagesSaved.get();
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
        imageSaveExecutor.submit(() -> {
            System.out.println("starting image save executor");
            long startTime = System.nanoTime();
            for (int i = 0; i < images.size(); i++) {
                Imgcodecs.imwrite(Path.of(folder, i + ".png").toString(), images.get(i));
            }
            saveState = SaveState.READY;
            System.out.println("number of raw frames grabbed: " + cameraImageGrabber.getNumFramesGrabbed());
            System.out.println("saved images to folder: " + folder);
            System.out.println("time to save " + images.size() + " images: " + (System.nanoTime() - startTime) / 1_000_000L);
            System.out.println("image index cap: " + imageIndexCap);
        });
    }
    public void clearOldImageData() {
        if (sendState == SendState.IN_PROGRESS)
            throw new IllegalStateException("cannot reset image index in CameraManager when it has not finished SENDING old images");
        if (saveState == SaveState.IN_PROGRESS)
            throw new IllegalStateException("cannot reset image index in CameraManager when it has not finished SAVING old images");

        numCameraImagesSaved.set(0);
        sendIndex.set(0);
        images.clear();
    }
    public int getImageIndexCap() {
        return imageIndexCap;
    }
    public void setImageIndexCap(int maxIndex) {
        this.imageIndexCap = maxIndex;
    }
    //    public void fillImagesToCap() {
//        int failedAttempts = 0;
//        while (numCameraImagesSaved.get() <= imageIndexCap && failedAttempts < 10) {
//            if (trySendImage())
//                numCameraImagesSaved.set(numCameraImagesSaved.get() + 1);
//            else
//                failedAttempts++;
//        }
//    }
    public void trySetDevicePort(int devicePort) {
        this.devicePort = devicePort;
        cameraImageGrabber.attemptReconnect(devicePort);
        connected = cameraImageGrabber.isConnected();
    }

    // start grabbing images on a separate thread and read and store the images at a fixed interval
    public void startReadingImagesFromCamera(long updateTimeNano) {
        Thread updateThread = new Thread(() -> {
            long nextFrameTime = System.nanoTime();
            double lastFrameTime = System.currentTimeMillis();
            numTimesSleptBetweenCameraReads = 0;
            totalTimeSleepingBetweenCameraReads = 0;
            totalTimeToReadImageFromCamera = 0;
            numTimesUpdatedCameraRead = 0;
            numTimesActuallyAddedImage = 0;
            maxSleepTime = 0;
            while (saveImage) {
                double curTime = System.currentTimeMillis();
                double sleepTime = curTime - lastFrameTime;
                maxSleepTime = Math.max(sleepTime, maxSleepTime);
                totalTimeSleepingBetweenCameraReads += sleepTime;
                lastFrameTime = curTime;

                updateImageReadingFromCamera();
                numTimesUpdatedCameraRead++;

                nextFrameTime += updateTimeNano;
                long sleepNanos = nextFrameTime - System.nanoTime();

                if (sleepNanos > 0) {
                    try {
                        TimeUnit.NANOSECONDS.sleep(sleepNanos);
                        numTimesSleptBetweenCameraReads++;
                    } catch (InterruptedException ignored) {}
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    // loads the images from the camera to the images list once
    public void updateImageReadingFromCamera() {
        if (!recording || !connected)
            return;

        System.out.println("updating image reading from camera");
        Mat latestFrame = cameraImageGrabber.getLatestFrame();
        if (!saveImage)
            System.out.println("SAVE IMAGE = FALSE");
        if (latestFrame == null)
            System.out.println("LATEST FRAME IS NULL");

        // saving images
        if(saveImage && latestFrame != null) {
            numCameraImagesSaved.set(numCameraImagesSaved.get() + 1);
            numTimesActuallyAddedImage++;
            images.add(latestFrame.clone());
            if (numCameraImagesSaved.get() > imageIndexCap && imageIndexCap > 0) {
                // turn off camera once enough images are saved
                saveImage = false;
                cameraImageGrabber.stopGrabbing();
            }
        }
    }
    // sends the currently stored images in the images list to the SSD for it to analyze
    private void updateImageSending() {
        if(sendIndex.get() < images.size()) {
            if (trySendImage()) {
                sendIndex.set(sendIndex.get() + 1);
            }
        }
        if (sendIndex.get() > imageIndexCap) // finished sending all
            sendState = SendState.READY;
        else if (sendIndex.get() >= images.size() && !saveImage) { // also finished sending, but images are incomplete
            sendState = SendState.READY_IMPERFECTLY;
            imageSendHandler.cancel(true);
        }
        if (finishedSendingImagesToSSD()) {
            System.out.println("number of times called updateImageReadingFromCamera: " + numTimesUpdatedCameraRead);
            System.out.println("number of times actually added image: " + numTimesActuallyAddedImage);
            System.out.println("average ms sleeping between camera reads: " + totalTimeSleepingBetweenCameraReads / numTimesSleptBetweenCameraReads);
            System.out.println("total ms sleeping between camera reads: " + totalTimeSleepingBetweenCameraReads);
            System.out.println("max ms sleeping between camera reads: " + maxSleepTime);
            System.out.println("average time to read " + getNumImagesSentToSSD() + " images from camera: " + totalTimeToReadImageFromCamera / getNumImagesSentToSSD());
            System.out.println("total time to read " + getNumImagesSentToSSD() + " image from camera: " + totalTimeToReadImageFromCamera);
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
        cameraImageGrabber.stopGrabbing();
    }
    public boolean isSavingImages() {
        return saveImage;
    }
    public void setSaveImage(boolean saveImage) {
        this.saveImage = saveImage;
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

    public Image getLatestImage() {
        if (cameraImageGrabber.getLatestFrame().empty())
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
    public int getNumImagesSentToSSD() {
        return sendIndex.get() - 1;
    }
    public CameraImageGrabber getImageGrabber() {
        return cameraImageGrabber;
    }
}
