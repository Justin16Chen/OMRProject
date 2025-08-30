package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.example.trialControlPanel.parentClasses.Core;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VisualizedImageReader {
    private final Core core;
    private final ScheduledExecutorService executor;
    private DataInputStream visualizedImageDataIn;
    public VisualizedImageReader(Core core) {
        this.core = core;
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    private boolean connectInputStream() {
        try (ServerSocket serverSocket = new ServerSocket(core.getProgramInfoWriter().getSocketPort())) {
            Socket clientSocket = serverSocket.accept();
            visualizedImageDataIn = new DataInputStream(clientSocket.getInputStream());
            return true;
        } catch (IOException e) {
            System.out.println("failed to connect input stream in VisualizedImageReader.java");
        }
        return false;
    }

    public void start(long intervalNanos) {
        if (!connectInputStream())
            return;

        executor.scheduleAtFixedRate(() -> {
            try {
                byte[] imgBytes = new byte[visualizedImageDataIn.readInt()];
                visualizedImageDataIn.readFully(imgBytes);
                ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                Platform.runLater(() -> core.getRunTrialController().updateCameraImageView(new Image(bais)));
            } catch (IOException e) {
                System.out.println("failed to read image from input stream");
            }
        }, 0, intervalNanos, TimeUnit.NANOSECONDS);
    }

    public void stop() {
        executor.shutdown();
    }
}
