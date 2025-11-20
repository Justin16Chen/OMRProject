package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.example.cameraCode.CameraManager;
import org.example.localProperties.LocalPropsReader;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.trialConfig.TrialSaver;
import org.example.trialControlPanel.utils.FilteredTextField;
import org.example.trialControlPanel.utils.TimeTextField;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

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
            updateButtonsEnabled()
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

        cameraOutputTextArea.setText(getCore().getJsonManager().getLastCameraOutputPath());
        visualizedOutputTextArea.setText(getCore().getJsonManager().getLastVisualizedOutputPath());
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
        updateButtonsEnabled();

        if (cm.isConnected())
            new CameraPreviewApplication(getCore()).start(new Stage());
    }

    @FXML
    private TextArea cameraOutputTextArea, visualizedOutputTextArea;
    @FXML
    private void handleCameraOutputClick() {
        String path = promptUserForEmptyFolderPath(getCore().getJsonManager().getLastCameraOutputPath());
        if (path != null) {
            cameraOutputTextArea.setText(path);
            getCore().getJsonManager().setCameraOutputPath(path);
        }
    }
    public String getCameraOutputPath() {
        return cameraOutputTextArea.getText();
    }
    @FXML
    private void handleVisualizedOutputClick() {
        String path = promptUserForEmptyFolderPath(getCore().getJsonManager().getLastVisualizedOutputPath());
        if (path != null) {
            visualizedOutputTextArea.setText(path);
            getCore().getJsonManager().setVisualizedOutputPath(path);
        }
    }
    public String getVisualizedOutputPath() {
        return visualizedOutputTextArea.getText();
    }
    private String promptUserForEmptyFolderPath(String startingPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select an Empty Folder");
        if (startingPath != null && Files.exists(Path.of(startingPath)))
            directoryChooser.setInitialDirectory(new File(startingPath));
        File file;
        do {
            file = directoryChooser.showDialog(getStage());
            if (file == null)
                return null;
            File[] nonDirs = file.listFiles(f -> !f.isDirectory());
            if (nonDirs == null)
                return null;
            if (nonDirs.length == 0)
                return file.getAbsolutePath();
        } while (true);
    }

    @FXML
    private Label startMenuMonitorNumberLabel, startMenuMonitorResolutionLabel, startMenuMonitorSizeLabel;
    @FXML
    private Label chamberMonitorNumberLabel, chamberMonitorResolutionLabel, chamberMonitorSizeLabel;

    public void updateButtonsEnabled() {
        if (queuedTrialNames.isEmpty()) {
            clearQueuedTrialsButton.setDisable(true);
            runQueueButton.setDisable(true);
            return;
        }
        clearQueuedTrialsButton.setDisable(false);

        if (cameraOutputTextArea.getText().equals("not specified") || visualizedOutputTextArea.getText().equals("not specified") || !cameraPortTextField.hasValidInput())
            runQueueButton.setDisable(true);
        else
            runQueueButton.setDisable(false);
    }
}
