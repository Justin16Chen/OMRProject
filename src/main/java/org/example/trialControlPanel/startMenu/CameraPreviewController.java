package org.example.trialControlPanel.startMenu;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.trialControlPanel.parentClasses.CustomController;


public class CameraPreviewController extends CustomController {
    @FXML
    private ImageView cameraImageView;


    @Override
    public void setup() {
        getCore().getCameraManager().startRecording();
        getCore().getCameraManager().setSaveImage(false);

        AnimationTimer updateCameraTimer = new AnimationTimer() {
            @Override
            public void handle(long l) {
                getCore().getCameraManager().update();
                Image image = getCore().getCameraManager().getLatestImage();
                if (image != null)
                    cameraImageView.setImage(image);
            }
        };
        updateCameraTimer.start();

        getStage().setOnCloseRequest(e -> updateCameraTimer.stop());
    }
}
