package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.trialDataManager.TrialDataManager;

import java.util.ArrayList;

public class QueueTrialController {

    private StartMenuController startMenuController;
    private QueueTrialApplication application;

    public void setApplication(QueueTrialApplication application) {
        this.application = application;
    }
    public void setStartMenuController(StartMenuController controller) {
        startMenuController = controller;
    }

    @FXML
    private ComboBox<TrialConfig> savedTrialsComboBox;
    public void updateSavedTrialsComboBox() {
        savedTrialsComboBox.getItems().clear();
        for (TrialConfig trialConfig : TrialDataManager.getSavedTrials())
            savedTrialsComboBox.getItems().add(trialConfig);
    }

    @FXML
    private Button addTrialToQueue;
    @FXML
    private void handleAddTrialToQueueClick() {
        TrialDataManager.getQueuedTrials().add(savedTrialsComboBox.getValue());
        startMenuController.updateQueuedTrialsTextArea();
        application.closeApplication();
    }
}
