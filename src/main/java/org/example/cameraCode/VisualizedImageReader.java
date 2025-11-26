package org.example.cameraCode;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.example.integration.JsonManager;
import org.example.trialControlPanel.parentClasses.Core;

import java.awt.image.RenderedImage;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VisualizedImageReader {
    public enum State {
        RECEIVING,
        FINISHED_RECEIVING_IMAGES
    }
    private final Core core;
    private DataInputStream visualizedImageDataIn;
    private boolean connected;
    private final ArrayList<WritableImage> receivedImages;
    public final ArrayList<Boolean> omrDetected;
    public final ArrayList<Double> headAngles;
    public final ArrayList<Double> tailAngles;
    private volatile State state;
    public VisualizedImageReader(Core core) {
        this.core = core;
        connected = false;
        receivedImages = new ArrayList<>();
        omrDetected = new ArrayList<>();
        headAngles = new ArrayList<>();
        tailAngles = new ArrayList<>();
        state = State.FINISHED_RECEIVING_IMAGES;
    }

    public void connectInputStream() {
        try (ServerSocket serverSocket = new ServerSocket(JsonManager.VISUALIZED_IMAGES_RECEIVER_PORT)) {
            Socket clientSocket = serverSocket.accept();
            visualizedImageDataIn = new DataInputStream(clientSocket.getInputStream());
            connected = true;
            System.out.println("visualized connected to python client");
        } catch (IOException e) {
            System.out.println("failed to connect input stream in VisualizedImageReader.java");
            connected = false;
        }
    }

    public void startReadingVisualizedImages() {
        if (!connected)
            throw new IllegalStateException("data input stream is not connected in VisualizedImageReader.java");

        receivedImages.clear();
        omrDetected.clear();
        headAngles.clear();
        tailAngles.clear();
        state = State.RECEIVING;

        new Thread(() -> {
            while (state == State.RECEIVING) {
                readVisualizedImages();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "receive visualized images thread").start();
    }

    private void readVisualizedImages() {
        try {
            byte[] header = visualizedImageDataIn.readNBytes(4);
            ByteBuffer bb = ByteBuffer.wrap(header);

            int omr = bb.getInt();
            boolean omrDetectedThisFrame = omr == 1;
            omrDetected.add(omrDetectedThisFrame);
            headAngles.add(-1.);
            tailAngles.add(-1.);

            int byteCount = core.getCameraManager().getFrameWidth() * core.getCameraManager().getFrameHeight() * 3;
            byte[] imgBytes = visualizedImageDataIn.readNBytes(byteCount);

            WritableImage wimg = new WritableImage(core.getCameraManager().getFrameWidth(), core.getCameraManager().getFrameHeight());
            PixelWriter pw = wimg.getPixelWriter();
            int idx = 0;
            for(int y = 0; y < core.getCameraManager().getFrameHeight(); y++)
                for(int x = 0; x < core.getCameraManager().getFrameWidth(); x++) {
                    int r = imgBytes[idx++] & 0xFF;
                    int g = imgBytes[idx++] & 0xFF;
                    int b = imgBytes[idx++] & 0xFF;
                    pw.setColor(x, y, Color.rgb(r, g, b));
                }

            receivedImages.add(wimg);

            // stop when all images have been received
            if (receivedImages.size() >= core.getCameraManager().getMaxImageIndex() + 1
                    || (receivedImages.size() >= core.getCameraManager().getSendIndex() && !core.getCameraManager().isReadingImagesFromCamera())) {
                state = State.FINISHED_RECEIVING_IMAGES;
            }
        } catch (IOException e) {
            System.out.println("failed to read image from input stream");
        }
    }

    public State getState() {
        return state;
    }

    public ArrayList<RenderedImage> getRenderedImagesSoFar() {
        System.out.println("formatting rendered images from last experiment");
        ArrayList<RenderedImage> output = new ArrayList<>();
        for (WritableImage img : receivedImages)
            output.add(SwingFXUtils.fromFXImage(img, null));
        return output;
    }

    public void stopRunning() {
        state = State.FINISHED_RECEIVING_IMAGES;
    }
    public WritableImage getLatestImage() {
        if (!receivedImages.isEmpty())
            return receivedImages.getLast();
        return null;
    }

    /*
    returns [numInstances, average duration millis, median millis]
    assumes that the reflex started AS early as possible
    (before the first frame it is detected but after the last frame it is not detected). should i assume this?
    */
    public static double[] getOMRInfo(ArrayList<Boolean> omrDetected, ArrayList<Double> timeStampsMs) {
        if (omrDetected.size() != timeStampsMs.size())
            throw new IllegalArgumentException("omr size of " + omrDetected.size() + " does not equal time stamp size of " + timeStampsMs.size());
        int numInstances = 0;
        ArrayList<Double> durationsMs = new ArrayList<>();
        durationsMs.add(0.);

        boolean cur = omrDetected.getFirst();
        boolean next;
        for (int i=0; i<omrDetected.size(); i++) {
            next = i < omrDetected.size() - 1 && omrDetected.get(i + 1);

            // update # instances
            if (cur && !next)
                numInstances++;

            // update duration
            if (cur) {
                if (i > 0) {
                    double timeDiffMs = timeStampsMs.get(i) - timeStampsMs.get(i - 1);
                    durationsMs.set(durationsMs.size() - 1, durationsMs.getLast() + timeDiffMs);
                }
                if (!next)
                    durationsMs.add(0.);
            }

            cur = next;
        }
        durationsMs.removeLast();

        System.out.println("durations: " + durationsMs);
        if (durationsMs.isEmpty())
            return new double[]{ 0, 0, 0 };

        List<Double> sortedDurationsMs = durationsMs.stream().sorted().toList();

        // calculate stats
        double totalDurationMs = durationsMs.stream().reduce(0., Double::sum);
        double avgDurationMs = totalDurationMs / numInstances;
        double medianIndex = durationsMs.size() * 0.5 - 0.5;
        System.out.println("median index: " + medianIndex);
        double median;
        if (durationsMs.size() % 2 == 1) {
            median = sortedDurationsMs.get((int) medianIndex);
            System.out.println("median normal: " + median);
        }
        else {
            double lowerVal = sortedDurationsMs.get((int) medianIndex);
            double upperVal = sortedDurationsMs.get((int) (medianIndex + 1));
            median = (lowerVal + upperVal) * 0.5;
            System.out.println("median average: " + median);
        }
        return new double[]{ numInstances, avgDurationMs, median };
    }

    public static void main(String[] args) {
        ArrayList<Boolean> omrDetected = new ArrayList<>(List.of(true, false, true, true, false, true, false, false, false, true, true, true, false));
        ArrayList<Double> timestampsMs = new ArrayList<>(List.of(1.,   40.,  62.,   86.,  110.,  130., 145.,  162.,  190.,  214., 240., 265., 291.));
        double[] results = getOMRInfo(omrDetected, timestampsMs);
        System.out.println(Arrays.toString(results));
    }
}
