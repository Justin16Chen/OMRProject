package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class RunTrialController extends CustomController {
    private static final DecimalFormat timeDf = new DecimalFormat("00");
    @FXML
    private Label nameLabel, stateLabel, trialLabel, cycleLabel, totalTimeLabel, testTimeLabel, restTimeLabel;
    @FXML
    private ProgressBar trialProgress, cycleProgress, testTimeProgress, restTimeProgress, totalTimeProgress;

    @FXML
    private Label cameraDataLabel;
    @FXML
    private ImageView cameraDataImageView;

    @Override
    public void setup() {
        getStage().setOnCloseRequest(e -> getCore().getOMRChamberController().stopTrial(true));
    }
    @FXML
    private void handleStopEarlyClick() {
        getCore().getOMRChamberController().stopTrial(true);
        getStage().close();
    }

    public void updateUILabels() {
        OMRChamberController chamberController = getCore().getOMRChamberController();
        Experiment trial = trials.get(chamberController.getCurrentTrialIndex());
        nameLabel.setText(trial.getName());
        stateLabel.setText("" + chamberController.getState());

        trialLabel.setText(chamberController.getCurrentTrialIndex() + 1 + "/" + trials.size());
        trialProgress.setProgress((chamberController.getCurrentTrialIndex() + 1.) / trials.size());

        cycleLabel.setText(chamberController.getCurrentCycle() + 1 + "/" + trial.getMaxTests());
        cycleProgress.setProgress((chamberController.getCurrentCycle() + 1.) / trial.getMaxTests());

        int totalTime = (int) chamberController.getTotalTime();
        totalTimeLabel.setText(formatSeconds((int) chamberController.getTotalSecondsRunning()) + "/" + formatSeconds(totalTime));
        totalTimeProgress.setProgress(Math.min(chamberController.getTotalSecondsRunning() / totalTime, 1) % 1);

        testTimeLabel.setText(formatSeconds((int) chamberController.getTestRunTime()) + "/" + formatSeconds(trial.getTestTime()));
        testTimeProgress.setProgress(Math.min(chamberController.getTestRunTime() / trial.getTestTime(), 1) % 1);

        restTimeLabel.setText(formatSeconds((int) chamberController.getRestRunTime()) + "/" + formatSeconds(trial.getRestTime()));
        restTimeProgress.setProgress(Math.min(chamberController.getRestRunTime() / trial.getRestTime(), 1) % 1);
    }

    public void updateCameraImageView(Image image) {
        cameraDataLabel.setText(image == null ? "Camera Data (None available)" : "Camera Data");
        cameraDataImageView.setImage(image);
    }

    private String formatSeconds(int seconds) {
        return timeDf.format(seconds / 60) + ":" + timeDf.format(seconds % 60);
    }

    private ArrayList<Experiment> trials;
    public void setTrials(ArrayList<Experiment> trials) {
        this.trials = trials;
    }
}
