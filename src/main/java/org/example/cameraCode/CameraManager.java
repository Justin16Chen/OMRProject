package org.example.cameraCode;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Properties;

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

    private VideoCapture cap;
    private String imageSavePath;
    private final Mat latestImage;
    private final ArrayList<Mat> images;
    private int index, maxIndex;
    private boolean connected, recording, saveImage;
    private int devicePort;
    private int updateNum;
    private double lastPrintTimeNano;

    private DataOutputStream outputStream;
    private final Core core;
    public final int width, height;

    public CameraManager(int devicePort, Core core) {
        this.devicePort = devicePort;
        this.core = core;

        latestImage = new Mat();
        images = new ArrayList<>();
        saveImage = true;
        index = 0;
        recording = false;

        cap = new VideoCapture(devicePort);
        connected = cap.isOpened();

        width = (int)cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
        height = (int)cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
    }
    public void connectOutputStream() {
        try (ServerSocket serverSocket = new ServerSocket(core.getProgramInfoWriter().getRawImagesPort())) {
            Socket clientSocket = serverSocket.accept();
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("camera connected to python client");
            ByteBuffer header = ByteBuffer.allocate(12);
            header.putInt(width);
            header.putInt(height);
            header.putInt(core.fps);
            outputStream.write(header.array());
            outputStream.flush();
            System.out.println("camera data sent to python client; width: " + width + ", height: " + height);
        } catch (IOException e) {
            System.out.println("camera failed to connect to python client");
            throw new RuntimeException(e);
        }
    }
    public int getImageIndex() {
        return index;
    }
    public void resetImageIndex() {
        index = 0;
    }
    public int getImageIndexCap() {
        return maxIndex;
    }
    public void setImageIndexCap(int maxIndex) {
        this.maxIndex = maxIndex;
    }
    public void fillImagesToCap() {
        int failedAttempts = 0;
        while (index <= maxIndex && failedAttempts < 10) {
            if (trySendImage())
                index++;
            else
                failedAttempts++;
        }
    }
    public void setImageSavePath(String path) {
        this.imageSavePath = path;
    }
    public void trySetDevicePort(int index) {
        cap = new VideoCapture(index);
        devicePort = index;
        connected = cap.isOpened();
    }
    public void update() {
        double time = System.currentTimeMillis();
        if (!recording || !connected)
            return;

        updateNum++;
        if (System.nanoTime() - lastPrintTimeNano > 250_000_000L) {
            lastPrintTimeNano = System.nanoTime();
//            System.out.println("saveImage: " + saveImage + " | FPS: " + updateNum * 4);
            updateNum = 0;
        }
//        long before = System.nanoTime();
        if(cap.read(latestImage)) {
            if (saveImage) {
                if (index > maxIndex && maxIndex > 0) {
                    saveImage = false;
                    return;
                }

                images.add(latestImage);
                if (trySendImage()) {
                    index++;
//                    System.out.println("time to save: " + (System.currentTimeMillis() - time));
                }
            }
        }
//        System.out.println("cm ms: " + (System.nanoTime() - before) / 1_000_000);
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
//        if (imageSavePath == null)
//            throw new IllegalStateException("when calling cameraManager.trySaveImage(), imageSavePath cannot be null");
        if(latestImage.empty())
            return false;
//        if (index > maxIndex && maxIndex != -1)
//            System.out.println("index is " + index + ", maxIndex is " + maxIndex);

        try {
            double before = System.currentTimeMillis();

            byte[] indexBytes = ByteBuffer.allocate(4).putInt(index).array();

            byte[] imgBytes = new byte[width * height * 3];
            latestImage.get(0, 0, imgBytes);
            outputStream.write(indexBytes);
            outputStream.write(imgBytes);
            outputStream.flush();
            System.out.println("camera data sent in " + (System.currentTimeMillis() - before) +"ms");
            return true;
        }
        catch (IOException e) {
            System.out.println("failed to send images");
            return false;
        }
//        Imgcodecs.imwrite(imageSavePath + "\\" + index + ".png", latestImage);
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
