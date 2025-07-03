package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.pattern.MonitorFormat;
import org.example.patternControlPanel.pattern.Pattern;
import org.example.patternControlPanel.pattern.PatternDirection;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.trialDataManager.TrialDataManager;

import java.util.ArrayList;
import java.util.List;

public class StartMenuController extends CustomController {

    private ArrayList<TrialConfig> queuedTrials;

    public ArrayList<TrialConfig> getQueuedTrials() {
        return queuedTrials;
    }

    private MonitorFormat startMenuMonitorFormat, OMRChamberMonitorFormat;
    public MonitorFormat getStartMenuMonitorFormat() {
        return startMenuMonitorFormat;
    }
    public void setStartMenuMonitorFormat(MonitorFormat mf) {
        startMenuMonitorFormat = mf;
    }

    @FXML
    private void initialize() {
        queuedTrialsTextArea.setEditable(false);
        queuedTrials = new ArrayList<>(List.of(new TrialConfig("defaultTrial", new Pattern("", PatternDirection.CLOCKWISE, 1, 1, 1, 0), 1, 1, 1, 1)));
        updateQueuedTrialsTextArea();
    }

    @FXML
    private void handleCreateTrialButtonClick() {
        getSceneManager().getPrimaryStage().setScene(getSceneManager().getTrialConfigScene());
    }

    @FXML
    private TextArea queuedTrialsTextArea;
    public void updateQueuedTrialsTextArea() {
        queuedTrialsTextArea.setText("");
        for (int i = 0; i< queuedTrials.size(); i++) {
            String newLine = i < queuedTrials.size() - 1 ? "\n" : "";
            queuedTrialsTextArea.setText(queuedTrialsTextArea.getText() + queuedTrials.get(i).name() + newLine);
        }
    }

    @FXML
    private void handleQueueTrialButtonClick() {
        QueueTrialApplication queueTrialApplication = new QueueTrialApplication(getSceneManager());
        queueTrialApplication.start(new Stage());
    }

    @FXML
    private void handleClearQueuedTrialsButtonClick() {
        queuedTrials.clear();
        updateQueuedTrialsTextArea();
    }
    @FXML
    private void handleRunQueueButtonClick() {
        int num = startMenuMonitorFormat.getMonitorNumber() + 1;
        if (num > MonitorFormat.getNumScreens())
            num = 1;
        getSceneManager().setupOMRChamberStage(new MonitorFormat(num), TrialDataManager.DEFAULT_TRIAL);
    }
}
