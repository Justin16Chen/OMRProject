package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.trialConfig.TrialSaver;
import org.example.trialControlPanel.utils.FilteredTextField;

import java.util.ArrayList;

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
        queuedTrialNames = new ArrayList<>();
        updateDefaultQueuedTrials();

        cameraPortTextField.setErrorMessage("Camera not found");
        cameraPortTextField.getTextField().setText("0");
        cameraPortTextField.setCheckInputType(FilteredTextField.CheckInputType.ON_COMMAND);
        cameraPortTextField.getTextField().textProperty().addListener((obs, o, n) -> {
            if (!o.equals(n))
                previewCameraButton.setDisable(false);
        });

        chamberMonitorNumberLabel.setText("");
        chamberMonitorResolutionLabel.setText("");
        chamberMonitorSizeLabel.setText("");
    }

    @Override
    public void setup() {
        cameraPortTextField.setValidationFunction(str -> FilteredTextField.VALID_INTEGER.test(str) && getCore().getCameraManager().isConnected());
        previewCameraButton.setDisable(!cameraPortTextField.hasValidInput());
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
        int num = startMenuMonitorFormat.getMonitorNumber() + 1;
        if (num > MonitorFormat.getNumScreens())
            num = 1;
        MonitorFormat chamberMonitorFormat = new MonitorFormat(num);

        chamberMonitorNumberLabel.setText("" + chamberMonitorFormat.getMonitorNumber());
        chamberMonitorResolutionLabel.setText(chamberMonitorFormat.getResolutionSpecs());
        chamberMonitorSizeLabel.setText(chamberMonitorFormat.getSizeSpecs());

        getCore().runOMRTrials(chamberMonitorFormat, TrialSaver.getTrial(queuedTrialNames.getFirst()));
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
