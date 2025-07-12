package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.trialConfig.TrialSaver;

import java.util.ArrayList;
import java.util.List;

public class StartMenuController extends CustomController {

    private ArrayList<String> queuedTrialNames;

    public ArrayList<String> getQueuedTrialNames() {
        return queuedTrialNames;
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
        queuedTrialNames = new ArrayList<>(List.of(TrialSaver.getAllTrialNames()[0]));
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
        for (int i = 0; i< queuedTrialNames.size(); i++) {
            String newLine = i < queuedTrialNames.size() - 1 ? "\n" : "";
            queuedTrialsTextArea.setText(queuedTrialsTextArea.getText() + queuedTrialNames.get(i) + newLine);
        }
    }

    @FXML
    private void handleQueueTrialButtonClick() {
        QueueTrialApplication queueTrialApplication = new QueueTrialApplication(getSceneManager());
        queueTrialApplication.start(new Stage());
    }

    @FXML
    private Button clearQueuedTrialsButton;
    @FXML
    private void handleClearQueuedTrialsButtonClick() {
        queuedTrialNames.clear();
        updateQueuedTrialsTextArea();
        updateButtonsEnabled();
    }
    @FXML
    private Button runQueueButton;
    @FXML
    private void handleRunQueueButtonClick() {
        int num = startMenuMonitorFormat.getMonitorNumber() + 1;
        if (num > MonitorFormat.getNumScreens())
            num = 1;
        MonitorFormat chamberMonitorFormat = new MonitorFormat(num);

        chamberMonitorNumberLabel.setText("" + chamberMonitorFormat.getMonitorNumber());
        chamberMonitorResolutionLabel.setText(chamberMonitorFormat.getResolutionSpecs());
        chamberMonitorSizeLabel.setText(chamberMonitorFormat.getSizeSpecs());

        getSceneManager().runOMRTrials(chamberMonitorFormat, TrialSaver.getTrial(queuedTrialNames.getFirst()));

    }

    @FXML
    private Label monitorNumberLabel, monitorResolutionLabel, monitorSizeLabel;
    @FXML
    private Label chamberMonitorNumberLabel, chamberMonitorResolutionLabel, chamberMonitorSizeLabel;

    public void updateButtonsEnabled() {
        if (queuedTrialNames.isEmpty()) {
            clearQueuedTrialsButton.setDisable(true);
            runQueueButton.setDisable(true);
        }
        else {
            clearQueuedTrialsButton.setDisable(false);
            runQueueButton.setDisable(false);
        }
    }
}
