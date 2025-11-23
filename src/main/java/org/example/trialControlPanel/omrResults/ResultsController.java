package org.example.trialControlPanel.omrResults;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.pattern.Pattern;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.util.ArrayList;

public class ResultsController extends CustomController {
    // for each experiment, a list of trial names and omr results
    private final ArrayList<ExperimentResult> experimentResults = new ArrayList<>();
    private final ArrayList<Integer> curTrialResults = new ArrayList<>();
    public boolean earlyStop = false;
    public void clearAllExperimentResults() {
        experimentResults.clear();
    }
    public void addTrialResult(int numOMR) {
        curTrialResults.add(numOMR);
    }
    public void finishExperiment(Experiment experiment, Pattern endingPattern) {
        ExperimentResult result = new ExperimentResult(experiment, curTrialResults);
        experimentResults.add(result);
        curTrialResults.clear();
    }
    @Override
    public void setup() {
        experimentResultsTextArea.setDisable(true);
        currentExperimentIndex = 0;

        if (!earlyStop)
            updateUIToNewExperiment();
    }

    @FXML
    private Label experimentNameLabel;
    @FXML
    private TextArea experimentResultsTextArea;
    @FXML
    private Label numTrialsUntilFailLabel;

    @FXML
    private Label speedLabel;
    @FXML
    private Label lightBandBrightnessLabel;
    @FXML
    private Label darkBandBrightnessLabel;
    @FXML
    private Label bandWidthLabel;
    @FXML
    private TextField saveFilePathTextField;

    private int currentExperimentIndex;
    @FXML
    private void handlePrevExperimentClick() {
        currentExperimentIndex++;
        updateUIToNewExperiment();
    }
    @FXML
    private void handleNextExperimentClick() {
        currentExperimentIndex--;
        updateUIToNewExperiment();
    }

    private void updateUIToNewExperiment() {
        ExperimentResult result = experimentResults.get(currentExperimentIndex);
        experimentNameLabel.setText(result.experiment.getName());

        experimentResultsTextArea.setText("");
        for (int i=0; i<result.getNumTrials(); i++) {
            String textToAdd =  "Trial " + (i + 1) + ": " + result.getNumOMRFromTrial(i);
            experimentResultsTextArea.setText(experimentResultsTextArea.getText() + textToAdd + "\n");
        }

        numTrialsUntilFailLabel.setText("Trials Until Failure: " + (result.getNumTrials() - 1));

        speedLabel.setText("" + result.experiment.getInitialPattern().getSpeed());
        lightBandBrightnessLabel.setText("" + result.experiment.getInitialPattern().getLightBrightness());
        darkBandBrightnessLabel.setText("" + result.experiment.getInitialPattern().getDarkBrightness());
        bandWidthLabel.setText("" + result.experiment.getInitialPattern().getBandWidth());
    }
}
