package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.trialConfig.TrialConfig;

public class RunningTrialInfoController extends CustomController {
    @FXML
    private Label nameLabel, testTimeLabel, restTimeLabel, totalTimeLabel;

    @FXML
    private ImageView cameraDataImageView;

    @FXML
    private Button stopEarlyButton;
    @FXML
    private void handleStopEarlyClick() {

    }

    @FXML
    private void initialize() {
        nameLabel.setText(trial.getName());
        testTimeLabel.setText(formatSeconds(getSceneManager().getOMRChamberController().getTestRunTime()) + "/" + formatSeconds(trial.getTestTime()));
        restTimeLabel.setText(formatSeconds(getSceneManager().getOMRChamberController().getRestRunTime()) + "/" + formatSeconds(trial.getRestTime()));
        totalTimeLabel.setText(formatSeconds(trial.getTotalTime()));
    }

    private String formatSeconds(double seconds) {
        return seconds / 60 + ":" + Math.round(seconds % 60);
    }

    private TrialConfig trial;
    public void setTrial(TrialConfig trial) {
        this.trial = trial;
    }
}
