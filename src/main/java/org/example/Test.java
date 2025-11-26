package org.example;

import org.example.cameraCode.CameraManager;
import org.example.cameraCode.VisualizedImageReader;
import org.example.trialControlPanel.parentClasses.Core;

import java.io.IOException;

public class Test {
    public static void main(String[] args) {
        Core core = new Core();
        CameraManager cm = core.getCameraManager();
        VisualizedImageReader vs = new VisualizedImageReader(core);
        cm.setMaxImageIndex(5);
        try {
            core.getSocketManager().connectOutputStream();
            core.getSocketManager().writeHeaderData(cm.getFrameWidth(), cm.getFrameHeight(), core.fps);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        vs.connectInputStream();

        cm.getImageGrabber().startGrabbing();
        long updateIntervalNanos = (long) (1e9 / core.fps);
        cm.startReadingImagesFromCamera(updateIntervalNanos);
        cm.startSendingImagesToSSD();
        vs.startReadingVisualizedImages();
    }
}
