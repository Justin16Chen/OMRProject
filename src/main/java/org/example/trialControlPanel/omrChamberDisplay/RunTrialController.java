package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.trialConfig.TrialConfig;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class RunTrialController extends CustomController {
    private static final DecimalFormat timeDf = new DecimalFormat("00");
    @FXML
    private Label nameLabel, cycleLabel, stateLabel, testTimeLabel, restTimeLabel, totalTimeLabel;
    @FXML
    private ProgressBar cycleProgress, testTimeProgress, restTimeProgress, totalTimeProgress;

    @FXML
    private Label cameraDataLabel;
    @FXML
    private ImageView cameraDataImageView;

    @Override
    public void setup() {
        getStage().setOnCloseRequest(e -> getCore().getOMRChamberController().stopTrial());
    }
    @FXML
    private void handleStopEarlyClick() {
        getCore().getOMRChamberController().stopTrial();
        getStage().close();
    }

    public void updateUILabels() {
        OMRChamberController chamberController = getCore().getOMRChamberController();
        TrialConfig trial = trials.get(chamberController.getCurrentTrialIndex());
        nameLabel.setText(trial.getName());
        stateLabel.setText("" + chamberController.getState());

        cycleLabel.setText(chamberController.getCurrentCycle() + 1 + "/" + trial.getMaxTests());
        cycleProgress.setProgress((1.0 * chamberController.getCurrentCycle() + 1) / trial.getMaxTests());

        testTimeLabel.setText(formatSeconds((int) chamberController.getTestRunTime()) + "/" + formatSeconds(trial.getTestTime()));
        testTimeProgress.setProgress(chamberController.getTestRunTime() / trial.getTestTime());

        restTimeLabel.setText(formatSeconds((int) chamberController.getRestRunTime()) + "/" + formatSeconds(trial.getRestTime()));
        restTimeProgress.setProgress(chamberController.getRestRunTime() / trial.getRestTime());

        totalTimeLabel.setText(formatSeconds((int) chamberController.getTotalSecondsRunning()) + "/" + formatSeconds(trial.getTotalTime()));
        totalTimeProgress.setProgress(chamberController.getTotalSecondsRunning() / trial.getTotalTime());
    }

    public void updateCameraImageView(Image image) {
        cameraDataLabel.setText(image == null ? "Camera Data (None available)" : "Camera Data");
        cameraDataImageView.setImage(image);

    }

    private String formatSeconds(int seconds) {
        return timeDf.format(seconds / 60) + ":" + timeDf.format(seconds % 60);
    }

    private ArrayList<TrialConfig> trials;
    public void setTrials(ArrayList<TrialConfig> trials) {
        this.trials = trials;
    }
}
