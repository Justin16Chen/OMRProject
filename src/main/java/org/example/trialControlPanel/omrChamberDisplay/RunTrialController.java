package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.text.DecimalFormat;

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
        getStage().setOnCloseRequest(e -> {
            System.out.println("closing run trial controller");
            getCore().getOmrChamberController().stopTrial(true);
        });
        showRawImages = true;
    }
    @FXML
    private void handleStopEarlyClick() {
        System.out.println("early stop click");
        getCore().getOmrChamberController().stopTrial(true);
        getStage().close();
    }
    private boolean showRawImages;
    @FXML
    private Button toggleImageViewButton;
    @FXML
    private void handleToggleImageView() {
        showRawImages = !showRawImages;
        toggleImageViewButton.setText(showRawImages ? "Show Raw Images" : "Show Annotated Images");
    }

    public void updateUILabels() {
        Experiment experiment = displaySM.getCurExperiment();
        nameLabel.setText(experiment.getName());
        stateLabel.setText("" + displaySM.getState());

        trialLabel.setText(displaySM.getCurExperimentIndex() + 1 + "/" + displaySM.getNumExperiments());
        trialProgress.setProgress((displaySM.getCurExperimentIndex() + 1.) / displaySM.getNumExperiments());

        cycleLabel.setText((displaySM.getCurTrial() + 1) + "/" + experiment.getMaxTests());
        cycleProgress.setProgress((displaySM.getCurTrial() + 1.) / experiment.getMaxTests());

        int totalTime = (int) displaySM.getTotalTime();
        totalTimeLabel.setText(formatSeconds((int) displaySM.getTotalSecondsRunning()) + "/" + formatSeconds(totalTime));
        totalTimeProgress.setProgress(Math.min(displaySM.getTotalSecondsRunning() / totalTime, 1) % 1);

        testTimeLabel.setText(formatSeconds((int) displaySM.getTestRunTime()) + "/" + formatSeconds(experiment.getTestTime()));
        testTimeProgress.setProgress(Math.min(displaySM.getTestRunTime() / experiment.getTestTime(), 1) % 1);

        restTimeLabel.setText(formatSeconds((int) displaySM.getRestRunTime()) + "/" + formatSeconds(experiment.getRestTime()));
        restTimeProgress.setProgress(Math.min(displaySM.getRestRunTime() / experiment.getRestTime(), 1) % 1);

        updateCameraImageView();
    }

    public void updateCameraImageView() {
        Image image = showRawImages ? getCore().getCameraManager().getLatestImage() : getCore().getOmrChamberController().visualizedImageReader.getLatestImage();
        cameraDataLabel.setText(image == null ? "Camera Data (None available)" : "Camera Data");
        cameraDataImageView.setImage(image);
    }

    private String formatSeconds(int seconds) {
        return timeDf.format(seconds / 60) + ":" + timeDf.format(seconds % 60);
    }

    private DisplayStateManager displaySM;
    public void setDisplaySM(DisplayStateManager displaySM) {
        this.displaySM = displaySM;
    }
}
