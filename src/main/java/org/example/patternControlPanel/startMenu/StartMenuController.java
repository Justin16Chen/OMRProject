package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.trialConfig.TrialConfigApplication;
import org.example.patternControlPanel.trialDataManager.TrialDataManager;

import java.util.ArrayList;

public class StartMenuController {

    @FXML
    private void initialize() {
        queuedTrialsTextArea.setEditable(false);
        updateQueuedTrialsTextArea();
    }

    @FXML
    private Button createTrialButton;
    @FXML
    private void handleCreateTrialButtonClick() {
        new TrialConfigApplication().start(new Stage());
    }

    @FXML
    private TextArea queuedTrialsTextArea;
    public void updateQueuedTrialsTextArea() {
        queuedTrialsTextArea.setText("");
        for (int i = 0; i< TrialDataManager.getQueuedTrials().size(); i++) {
            String newLine = i < TrialDataManager.getQueuedTrials().size() - 1 ? "\n" : "";
            queuedTrialsTextArea.setText(queuedTrialsTextArea.getText() + TrialDataManager.getQueuedTrials().get(i).name() + newLine);
        }
    }

    @FXML
    private Button queueTrialButton;
    @FXML
    private void handleQueueTrialButtonClick() {
        QueueTrialApplication queueTrialApplication = new QueueTrialApplication();
        queueTrialApplication.start(new Stage());
        queueTrialApplication.getController().updateSavedTrialsComboBox();
        queueTrialApplication.getController().setStartMenuController(this);
    }

    @FXML
    private Button runQueueButton;
    @FXML
    private void handleRunQueueButtonClick() {

    }
}
