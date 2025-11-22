package org.example.trialControlPanel.omrResults;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.example.trialControlPanel.parentClasses.CustomController;

public class ExperimentResultsController extends CustomController {
    @Override
    public void setup() {
        currentExperimentIndex = 0;
        updateUIToNewExperiment();
    }

    @FXML
    private Label curExperimentNameLabel;
    @FXML
    private TextArea curExperimentResultsTextArea;
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

    }
}
