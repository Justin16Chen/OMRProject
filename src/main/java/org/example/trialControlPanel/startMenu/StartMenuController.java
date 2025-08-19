package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.cameraCode.CameraManager;
import org.example.localProperties.LocalPropsReader;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.trialConfig.TrialSaver;
import org.example.trialControlPanel.utils.FilteredTextField;
import org.example.trialControlPanel.utils.TimeTextField;

import java.util.ArrayList;

public class StartMenuController extends CustomController {

    private ArrayList<String> queuedTrialNames;
    public ArrayList<String> getQueuedTrialNames() {
        return queuedTrialNames;
    }

    private MonitorFormat startMenuMonitorFormat;
    public MonitorFormat getStartMenuMonitorFormat() {
        return startMenuMonitorFormat;
    }
    public void setStartMenuMonitorFormat(MonitorFormat mf) {
        startMenuMonitorFormat = mf;
        startMenuMonitorNumberLabel.setText("" + mf.getMonitorNumber());
        startMenuMonitorResolutionLabel.setText(mf.getResolutionSpecs());
        startMenuMonitorSizeLabel.setText(mf.getSizeSpecs());
    }

    @FXML
    private void initialize() {
        queuedTrialsTextArea.setEditable(false);
        queuedTrialNames = new ArrayList<>();
        updateDefaultQueuedTrials();

        cameraPortTextField.setErrorMessage("Camera not found");
        cameraPortTextField.getTextField().setText("0");
        cameraPortTextField.setCheckInputType(FilteredTextField.CheckInputType.ON_COMMAND);
        cameraPortTextField.getTextField().textProperty().addListener((obs, o, n) -> {
            if (!o.equals(n))
                previewCameraButton.setDisable(false);
        });
        restTimeTextField.getTextField().textProperty().addListener((obs, oldVal, newVal) ->
            runQueueButton.setDisable(!restTimeTextField.hasValidInput())
        );
        startMenuMonitorNumberLabel.setText("Not set");
        startMenuMonitorResolutionLabel.setText("Not set");
        startMenuMonitorSizeLabel.setText("Not set");
        chamberMonitorNumberLabel.setText("");
        chamberMonitorResolutionLabel.setText("");
        chamberMonitorSizeLabel.setText("");

    }

    @Override
    public void setup() {
        cameraPortTextField.setValidationFunction(str -> FilteredTextField.VALID_INTEGER.test(str) && getCore().getCameraManager().isConnected());
        previewCameraButton.setDisable(!cameraPortTextField.hasValidInput());
        getCore().getStartMenuMonitorManager().updateMonitorFormat(getCore().getPrimaryStage());
    }

    private void updateDefaultQueuedTrials() {
        queuedTrialNames.clear();
        if (TrialSaver.getAllTrialNames().length > 0)
            queuedTrialNames.add(TrialSaver.getAllTrialNames()[0]);
        updateQueuedTrialsTextArea();
    }

    @FXML
    private void handleCreateTrialButtonClick() {
        getCore().getPrimaryStage().setScene(getCore().getTrialConfigScene());
        getCore().getTrialConfigController().initialize();
    }
    @FXML
    private void handleEditTrialButtonClick() {
        getCore().getPrimaryStage().setScene(getCore().getTrialConfigScene());
        getCore().getTrialConfigController().handleEditClick();
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
    private TimeTextField restTimeTextField;

    @FXML
    private void handleQueueTrialButtonClick() {
        QueueTrialApplication queueTrialApplication = new QueueTrialApplication(getCore());
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
        int numScreens = MonitorFormat.getNumScreens();
        int num = startMenuMonitorFormat.getMonitorNumber() + 1;
        if (num > numScreens)
            num -= numScreens;
        MonitorFormat[] chamberMonitors = new MonitorFormat[Core.NUM_OMR_CHAMBER_CHILDREN + 1];
        for (int i=0; i<Core.NUM_OMR_CHAMBER_CHILDREN + 1; i++) {
            int monitorNum = num + i;
            if (monitorNum > numScreens)
                monitorNum -= numScreens;
            chamberMonitors[i] = new MonitorFormat(monitorNum);
            if (LocalPropsReader.shouldUsePrespecifiedChamberMonitorSize())
                chamberMonitors[i].setPhysicalSize(LocalPropsReader.getChamberMonitorWidthCm(), LocalPropsReader.getChamberMonitorHeightCm());
        }
        chamberMonitorNumberLabel.setText("" + chamberMonitors[0].getMonitorNumber());
        chamberMonitorResolutionLabel.setText(chamberMonitors[0].getResolutionSpecs());
        chamberMonitorSizeLabel.setText(chamberMonitors[0].getSizeSpecs());
        getCore().runOMRTrials(chamberMonitors, TrialSaver.getTrials(queuedTrialNames), restTimeTextField.getSeconds());
        System.out.println(queuedTrialNames);
    }

    @FXML
    private FilteredTextField cameraPortTextField;
    @FXML
    private Button previewCameraButton;
    @FXML
    private void handlePreviewCameraButtonClick() {
        CameraManager cm = getCore().getCameraManager();
        try {
            int portIndex = cameraPortTextField.getIntegerInput();
            if (portIndex != cm.getDevicePort()) {
                cm.trySetDevicePort(portIndex);
                previewCameraButton.setDisable(!cm.isConnected());
            }
        } catch (NumberFormatException e) {
            previewCameraButton.setDisable(true);
        }
        cameraPortTextField.hasValidInput(); // update error on text field

        if (cm.isConnected())
            new CameraPreviewApplication(getCore()).start(new Stage());
    }

    @FXML
    private Label startMenuMonitorNumberLabel, startMenuMonitorResolutionLabel, startMenuMonitorSizeLabel;
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
