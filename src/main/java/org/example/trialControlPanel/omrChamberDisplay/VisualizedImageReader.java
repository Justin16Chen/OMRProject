package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.example.integration.JsonManager;
import org.example.trialControlPanel.parentClasses.Core;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.*;

public class VisualizedImageReader {
    public enum State {
        WAITING_TO_RECEIVE,
        RECEIVING,
        WAITING_TO_SAVE,
        SAVING
    }
    private final Core core;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> executorHandler;
    private final ExecutorService saveExecutor;
    private DataInputStream visualizedImageDataIn;
    private boolean connected;
    private final ArrayList<WritableImage> receivedImages;
    private volatile State state;
    public VisualizedImageReader(Core core) {
        this.core = core;
        executor = Executors.newSingleThreadScheduledExecutor();
        saveExecutor = Executors.newSingleThreadScheduledExecutor();
        connected = false;
        receivedImages = new ArrayList<>();
        state = State.WAITING_TO_RECEIVE;
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

    public void startReadingVisualizedImages(long intervalNanos) {
        if (!connected)
            throw new IllegalStateException("data input stream is not connected in VisualizedImageReader.java");

        receivedImages.clear();
        state = State.RECEIVING;

        executorHandler = executor.scheduleAtFixedRate(() -> {
            try {
//                double time = System.currentTimeMillis();
                byte[] header = visualizedImageDataIn.readNBytes(4);
//                System.out.println("time to read header: " + (System.currentTimeMillis() - time));
                ByteBuffer bb = ByteBuffer.wrap(header);
//                System.out.println("time to create buffer: " + (System.currentTimeMillis() - time));
                int omr = bb.getInt();

//                System.out.println("time to read w&h bytes: " + (System.currentTimeMillis() - time));

                int byteCount = core.getCameraManager().getFrameWidth() * core.getCameraManager().getFrameHeight() * 3;
                byte[] imgBytes = visualizedImageDataIn.readNBytes(byteCount);

//                System.out.println("time to read imgBytes: " + (System.currentTimeMillis() - time));

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

//                System.out.println("time to receive visualized img: " + (System.currentTimeMillis() - time));
                receivedImages.add(wimg);

                Platform.runLater(() -> core.getRunTrialController().updateCameraImageView(wimg));

                // stop when all images have been received
                if (receivedImages.size() >= core.getCameraManager().getImageIndexCap() + 1
                    || (receivedImages.size() >= core.getCameraManager().getSendIndex() && !core.getCameraManager().isSavingImages())) {
                    state = State.WAITING_TO_SAVE;
                    executorHandler.cancel(true);
                }
            } catch (IOException e) {
                System.out.println("failed to read image from input stream");
            }
        }, 0, intervalNanos, TimeUnit.NANOSECONDS);
    }

    public State getState() {
        return state;
    }
    public void saveAndClearStoredImages(String folderPath) {
        if (state != State.WAITING_TO_SAVE)
            throw new IllegalStateException("cannot call saveAndClearStoredImages in VisualizedImageReader when it is in state " + state);
        state = State.SAVING;
        saveExecutor.submit(() -> {
            for (int i=0; i<receivedImages.size(); i++) {
                RenderedImage renderedImage = SwingFXUtils.fromFXImage(receivedImages.get(i), null);

                // Write to file
                File file = Paths.get(folderPath, i + ".png").toFile();
                try {
                    ImageIO.write(renderedImage, "png", file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("finished saving " + receivedImages.size() + " images");
            receivedImages.clear();
            state = State.WAITING_TO_RECEIVE;
        });
    }

    public void stopRunning() {
        if (executorHandler != null && !executorHandler.isCancelled())
            executorHandler.cancel(true);
    }
    public void shutDownExecutor() {
        executor.shutdown();
        saveExecutor.shutdown();
    }
}
