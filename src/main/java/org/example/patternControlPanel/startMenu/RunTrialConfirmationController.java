package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.pattern.MonitorFormat;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.trialDataManager.TrialDataManager;

public class RunTrialConfirmationController extends CustomController {
    @FXML
    private void initialize() {
        for (int i=1; i<MonitorFormat.getNumScreens(); i++)
            availableMonitorsDropdown.getItems().add(i);
    }
    @FXML
    private ComboBox<Integer> availableMonitorsDropdown;
    @FXML
    private void handleRunClick() {
        getStage().close();
        getSceneManager().setupOMRChamberStage(new MonitorFormat(availableMonitorsDropdown.getValue()), TrialDataManager.DEFAULT_TRIAL);
    }
}
