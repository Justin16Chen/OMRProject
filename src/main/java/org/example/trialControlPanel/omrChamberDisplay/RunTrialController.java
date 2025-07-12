package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.trialConfig.TrialConfig;

public class RunTrialController extends CustomController {
    @FXML
    private Label nameLabel, cycleLabel, stateLabel, testTimeLabel, restTimeLabel, totalTimeLabel;

    @FXML
    private ImageView cameraDataImageView;

    @FXML
    private Button stopEarlyButton;
    @FXML
    private void handleStopEarlyClick() {

    }

    public void updateUI() {
        OMRChamberController chamberController = getSceneManager().getOMRChamberController();
        nameLabel.setText(trial.getName());
        cycleLabel.setText("" + chamberController.getCycle());
        stateLabel.setText("" + chamberController.getState());
        testTimeLabel.setText(formatSeconds(chamberController.getTestRunTime()) + "/" + formatSeconds(trial.getTestTime()));
        restTimeLabel.setText(formatSeconds(chamberController.getRestRunTime()) + "/" + formatSeconds(trial.getRestTime()));
        totalTimeLabel.setText(formatSeconds(chamberController.getTotalRunTime()) + "/" + formatSeconds(trial.getTotalTime()));
    }

    private String formatSeconds(int seconds) {
        return seconds / 60 + ":" + seconds % 60;
    }

    private TrialConfig trial;
    public void setTrial(TrialConfig trial) {
        this.trial = trial;
    }
}
