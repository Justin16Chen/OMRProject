package org.example;

import org.example.cameraCode.CameraManager;
import org.example.cameraCode.VisualizedImageReader;
import org.example.trialControlPanel.parentClasses.Core;

public class Test {
    public static void main(String[] args) {
        Core core = new Core();
        CameraManager cm = new CameraManager(0, core);
        cm.setMaxImageIndex(5);
//        VisualizedImageReader vr = new VisualizedImageReader(core);

        long updateIntervalNanos = (long) (1e9 / core.fps);
        cm.startReadingImagesFromCamera(updateIntervalNanos);
        cm.startSendingImagesToSSD();
//        vr.startReadingVisualizedImages();
    }
}
