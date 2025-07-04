package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
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
        monitorNumberLabel.setText("" + mf.getMonitorNumber());
        monitorResolutionLabel.setText(mf.getResolutionSpecs());
        monitorSizeLabel.setText(mf.getSizeSpecs());
    }

    @FXML
    private void initialize() {
        queuedTrialsTextArea.setEditable(false);
        queuedTrials = new ArrayList<>(List.of(new TrialConfig("defaultTrial", new Pattern("", PatternDirection.CLOCKWISE, 1, 1, 1, 0), 1, 1, 1, 1)));
        updateQueuedTrialsTextArea();
        chamberMonitorNumberLabel.setText("");
        chamberMonitorResolutionLabel.setText("");
        chamberMonitorSizeLabel.setText("");
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
        MonitorFormat chamberMonitorFormat = new MonitorFormat(num);
        getSceneManager().setupOMRChamberStage(chamberMonitorFormat, TrialDataManager.DEFAULT_TRIAL);

        chamberMonitorNumberLabel.setText("" + chamberMonitorFormat.getMonitorNumber());
        chamberMonitorResolutionLabel.setText(chamberMonitorFormat.getResolutionSpecs());
        chamberMonitorSizeLabel.setText(chamberMonitorFormat.getSizeSpecs());
    }

    @FXML
    private Label monitorNumberLabel, monitorResolutionLabel, monitorSizeLabel;
    @FXML
    private Label chamberMonitorNumberLabel, chamberMonitorResolutionLabel, chamberMonitorSizeLabel;
}
