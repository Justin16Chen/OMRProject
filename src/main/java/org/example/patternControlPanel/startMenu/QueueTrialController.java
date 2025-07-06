package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.pattern.TrialSaver;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.trialConfig.TrialConfig;

public class QueueTrialController extends CustomController {

    @FXML
    private ComboBox<String> savedTrialsComboBox;
    public void updateSavedTrialsComboBox() {
        savedTrialsComboBox.getItems().clear();
        for (String trialName : TrialSaver.getAllTrialNames())
            savedTrialsComboBox.getItems().add(trialName);
    }

    @FXML
    private void handleAddTrialToQueueClick() {
        getSceneManager().getStartMenuController().getQueuedTrials().add(TrialSaver.getTrial(savedTrialsComboBox.getValue()));
        getSceneManager().getStartMenuController().updateQueuedTrialsTextArea();
        getStage().close();
    }
}
