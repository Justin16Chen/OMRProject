package org.example.trialControlPanel.startMenu;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.trialControlPanel.parentClasses.CustomController;


public class CameraPreviewController extends CustomController {
    @FXML
    private ImageView cameraImageView;

    private AnimationTimer updateCameraTimer;
    @Override
    public void setup() {
        getCore().getCameraManager().getImageGrabber().startGrabbing();
        updateCameraTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                Image image = getCore().getCameraManager().getLatestImageFromGrabber();
                if (image != null)
                    cameraImageView.setImage(image);
            }
        };
        updateCameraTimer.start();

        getStage().setOnCloseRequest(e -> stop());
    }
    private void stop() {
        if (updateCameraTimer != null)
            updateCameraTimer.stop();
        getCore().getCameraManager().getImageGrabber().stopGrabbing();
    }
}
