package org.example.trialControlPanel.startMenu;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.trialControlPanel.parentClasses.CustomController;

public class LoadingController extends CustomController {
    public static final long updateMsInterval = 750;
    @FXML
    private Label loadingLabel;
    @Override
    public void setup() {
        new AnimationTimer() {
            private long lastTimeNano;
            private int numDots = 0;
            @Override
            public void handle(long now) {
                if ((now - lastTimeNano) / 1e6 < updateMsInterval)
                    return;
                lastTimeNano = now;
                loadingLabel.setText("Loading" + ".".repeat(numDots));
                numDots = (numDots + 1) % 4;
            }
        }.start();
    }
}
