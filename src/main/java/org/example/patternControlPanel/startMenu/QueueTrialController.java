package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.SceneManager.Controller;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.trialDataManager.TrialDataManager;

import java.util.ArrayList;

public class QueueTrialController extends Controller {

    private QueueTrialApplication application;

    public void setApplication(QueueTrialApplication application) {
        this.application = application;
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
        getSceneManager().getStartMenuController().getQueuedTrials().add(savedTrialsComboBox.getValue());
        getSceneManager().getStartMenuController().updateQueuedTrialsTextArea();
        application.closeApplication();
    }
}
