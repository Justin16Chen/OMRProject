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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class VisualizedImageReader {
    private final Core core;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> executorHandler;
    private DataInputStream visualizedImageDataIn;
    private boolean connected;
    private final ArrayList<WritableImage> receivedImages;
    public VisualizedImageReader(Core core) {
        this.core = core;
        executor = Executors.newSingleThreadScheduledExecutor();
        connected = false;
        receivedImages = new ArrayList<>();
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

    public void start(long intervalNanos) {
        if (!connected)
            throw new IllegalStateException("data input stream is not connected in VisualizedImageReader.java");

        receivedImages.clear();

        executorHandler = executor.scheduleAtFixedRate(() -> {
            try {
                double time = System.currentTimeMillis();
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

                System.out.println("time to receive visualized img: " + (System.currentTimeMillis() - time));

                receivedImages.add(wimg);

                Platform.runLater(() -> core.getRunTrialController().updateCameraImageView(wimg));
            } catch (IOException e) {
                System.out.println("failed to read image from input stream");
            }
        }, 0, intervalNanos, TimeUnit.NANOSECONDS);
    }

    public void clearStoredImages() {
        receivedImages.clear();
    }
    public void saveAndClearStoredImages(String folderPath) {
        for (int i=0; i<receivedImages.size(); i++) {
            RenderedImage renderedImage = SwingFXUtils.fromFXImage(receivedImages.get(i), null);

            // Write to file
            File file = Paths.get(folderPath, 0 + ".png").toFile();
            try {
                ImageIO.write(renderedImage, "png", file);
                System.out.println("Image saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        receivedImages.clear();
    }

    public void stopRunning() {
        if (executorHandler != null && !executorHandler.isCancelled())
        executorHandler.cancel(true);
    }
    public void shutDownExecutor() {
        executor.shutdown();
    }
}
