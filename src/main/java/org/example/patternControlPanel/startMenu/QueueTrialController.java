package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.trialDataManager.TrialDataManager;

public class QueueTrialController extends CustomController {

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
        getSceneManager().getStartMenuController().getQueuedTrials().add(savedTrialsComboBox.getValue());
        getSceneManager().getStartMenuController().updateQueuedTrialsTextArea();
        getStage().close();
    }
}
