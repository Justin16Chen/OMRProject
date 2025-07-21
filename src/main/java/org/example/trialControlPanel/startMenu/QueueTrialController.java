package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import org.example.trialControlPanel.trialConfig.TrialSaver;
import org.example.trialControlPanel.sceneManager.CustomController;

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
        getSceneManager().getStartMenuController().getQueuedTrialNames().add(savedTrialsComboBox.getValue());
        getSceneManager().getStartMenuController().updateQueuedTrialsTextArea();
        getSceneManager().getStartMenuController().updateButtonsEnabled();
        getStage().close();
    }
}
