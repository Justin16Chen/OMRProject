package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.example.trialControlPanel.parentClasses.Core;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
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
    public VisualizedImageReader(Core core) {
        this.core = core;
        executor = Executors.newSingleThreadScheduledExecutor();
        connected = false;
    }

    public void connectInputStream() {
        try (ServerSocket serverSocket = new ServerSocket(core.getProgramInfoWriter().getVisualizedImagesPort())) {
            Socket clientSocket = serverSocket.accept();
            visualizedImageDataIn = new DataInputStream(clientSocket.getInputStream());
            connected = true;
        } catch (IOException e) {
            System.out.println("failed to connect input stream in VisualizedImageReader.java");
            connected = false;
        }
    }

    public void start(long intervalNanos) {
        if (!connected)
            throw new IllegalStateException("data input stream is not connected in VisualizedImageReader.java");

        executorHandler = executor.scheduleAtFixedRate(() -> {
            try {

                double time = System.currentTimeMillis();
                byte[] header = visualizedImageDataIn.readNBytes(8);
//                System.out.println("time to read header: " + (System.currentTimeMillis() - time));
                ByteBuffer bb = ByteBuffer.wrap(header);
//                System.out.println("time to create buffer: " + (System.currentTimeMillis() - time));
                int width = bb.getInt();
                int height = bb.getInt();

//                System.out.println("time to read w&h bytes: " + (System.currentTimeMillis() - time));

                int byteCount = width * height * 3;
                byte[] imgBytes = visualizedImageDataIn.readNBytes(byteCount);

//                System.out.println("time to read imgBytes: " + (System.currentTimeMillis() - time));

                WritableImage wimg = new WritableImage(width, height);
                PixelWriter pw = wimg.getPixelWriter();
                int idx = 0;
                for(int y = 0; y < height; y++)
                    for(int x = 0; x < width; x++) {
                        int r = imgBytes[idx++] & 0xFF;
                        int g = imgBytes[idx++] & 0xFF;
                        int b = imgBytes[idx++] & 0xFF;
                        pw.setColor(x, y, Color.rgb(r, g, b));
                    }

//                System.out.println("time to receive img: " + (System.currentTimeMillis() - time));

//                byte[] imgBytes = new byte[visualizedImageDataIn.readInt()];
//                visualizedImageDataIn.readFully(imgBytes);
//                ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                Platform.runLater(() -> core.getRunTrialController().updateCameraImageView(wimg));
            } catch (IOException e) {
                System.out.println("failed to read image from input stream");
            }
        }, 0, intervalNanos, TimeUnit.NANOSECONDS);
    }

    public void stopRunning() {
        if (executorHandler != null && !executorHandler.isCancelled())
        executorHandler.cancel(true);
    }
    public void shutDownExecutor() {
        executor.shutdown();
    }
}
