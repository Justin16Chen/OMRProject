package org.example.trialControlPanel.startMenu;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.example.cameraCode.CameraManager;
import org.example.cameraCode.VisualizedImageReader;
import org.example.localProperties.LocalPropsReader;
import org.example.trialControlPanel.omrResults.ExperimentResult;
import org.example.trialControlPanel.omrResults.TrialResult;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.Pattern;
import org.example.trialControlPanel.trialConfig.Experiment;
import org.example.trialControlPanel.trialConfig.TrialSaver;
import org.example.trialControlPanel.utils.FilteredTextField;
import org.example.trialControlPanel.utils.TimeTextField;
import org.example.utils.VideoUtils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        restTimeTextField.getTextField().textProperty().addListener((obs, oldVal, newVal) -> updateButtonsEnabled());

        startMenuMonitorNumberLabel.setText("Not set");
        startMenuMonitorResolutionLabel.setText("Not set");
        startMenuMonitorSizeLabel.setText("Not set");
        chamberMonitorNumberLabel.setText("Not set");
        chamberMonitorResolutionLabel.setText("Not set");
        chamberMonitorSizeLabel.setText("Not set");
    }

    @Override
    public void setup() {
        System.out.println("SETUP FOR START MENU");
        cameraPortTextField.setValidationFunction(str -> FilteredTextField.VALID_INTEGER.test(str) && getCore().getCameraManager().isConnected());
        previewCameraButton.setDisable(!cameraPortTextField.hasValidInput());
        getCore().getStartMenuMonitorManager().updateMonitorFormat(getCore().getPrimaryStage());

        parentFolderTextField.setText(getCore().getJsonManager().getLastParentFolderPath());
        parentFolderTextField.textProperty().addListener((old, n, obs) -> {
            updateButtonsEnabled();
        });
        String invalidChars = "[\\\\/:*?\"<>|]";
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            String newText = change.getText();
            if (newText.matches(".*" + invalidChars + ".*")) {
                // Remove invalid characters from the inserted text
                change.setText(newText.replaceAll(invalidChars, ""));
            }
            return change;
        });
        testNameTextField.setTextFormatter(formatter);

        removeOutdatedQueuedTrials();
        updateButtonsEnabled();

    }

    private void updateDefaultQueuedTrials() {
        queuedTrialNames.clear();
        if (TrialSaver.getAllTrialNames().length > 0)
            queuedTrialNames.add(TrialSaver.getAllTrialNames()[0]);
        updateQueuedTrialsTextArea();
    }
    private void removeOutdatedQueuedTrials() {
        for (int i=0; i<queuedTrialNames.size(); i++)
            if (TrialSaver.getTrial(queuedTrialNames.get(i)) == null) {
                queuedTrialNames.remove(i);
                i--;
            }
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
        experimentResults.clear();
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
    private TextField parentFolderTextField, testNameTextField;
    @FXML
    public Label exportingLabel;
    @FXML
    private void handleChooseFolderClick() {
        String path = promptUserForFolderPath(getCore().getJsonManager().getLastParentFolderPath());
        if (path != null) {
            parentFolderTextField.setText(path);
            getCore().getJsonManager().setParentFolderPath(path);
        }
    }
    private String promptUserForFolderPath(String startingPath) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select an Empty Folder");
        if (startingPath != null && Files.exists(Path.of(startingPath)))
            directoryChooser.setInitialDirectory(new File(startingPath));
        File file;
        file = directoryChooser.showDialog(getStage());
        if (file == null)
            return null;
        return file.getAbsolutePath();
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

        boolean invalidFilePath = !Files.exists(Path.of(parentFolderTextField.getText())) || !cameraPortTextField.hasValidInput();
        boolean savingLastRunQueue = getCore().getCameraManager().getSendState() == CameraManager.SendState.IN_PROGRESS ||
                (getCore().getOmrChamberController().visualizedImageReader != null && getCore().getOmrChamberController().visualizedImageReader.getState() != VisualizedImageReader.State.FINISHED_RECEIVING_IMAGES);
        boolean invalidTestName = testNameTextField.getText().isEmpty();
        runQueueButton.setDisable(invalidFilePath || savingLastRunQueue || invalidTestName);
    }


    // for each experiment, a list of trial names and omr results
    // this list is automatically cleared everytime core.runOMRTrials() is called b/c it reloads the scene and controller from the FXML file
    private final ArrayList<ExperimentResult> experimentResults = new ArrayList<>();
    private ExperimentResult curExperimentResult;
    public void addTrialResult(ArrayList<Mat> rawImages, ArrayList<RenderedImage> visualizedImages, double[] results, ArrayList<Double> timestamps, ArrayList<Double> headAngles, ArrayList<Double> tailAngles) {
        if (curExperimentResult == null)
            curExperimentResult = new ExperimentResult();

        curExperimentResult.trialResults.add(new TrialResult(rawImages, visualizedImages, results, timestamps, headAngles, tailAngles));
    }
    public void finishExperiment(Experiment experiment, Pattern endingPattern) {
        System.out.println("finishing experiment");
        System.out.println("adding " + experiment);
        System.out.println("adding ending pattern: " + endingPattern);

        curExperimentResult.experiment = experiment;
        curExperimentResult.endingPattern = endingPattern;

        experimentResults.add(curExperimentResult);
        curExperimentResult = null;
    }


    public void saveAllResults() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_hhmmss"));
        String header =  testNameTextField.getText() + "_" + dateStr;
        String subfolderPath = Path.of(parentFolderTextField.getText(),header).toString();
        System.out.println("created subfolder " + header + ": " + new File(subfolderPath).mkdirs());

        saveResultsTxtFile(testNameTextField.getText(), dateStr, subfolderPath);
        for (int j=0; j<experimentResults.size(); j++) {
            ExperimentResult expResult = experimentResults.get(j);
            for (int i = 0; i < expResult.trialResults.size(); i++) {
                String rawPath = Path.of(subfolderPath, testNameTextField.getText() + "_exp" + (j + 1) + "_" + expResult.experiment.getName() + "_raw_trial" + (i + 1) + "_" + dateStr + ".mp4").toString();
                String visualizedPath = Path.of(subfolderPath, testNameTextField.getText() + "_exp" + (j + 1) + "_" + expResult.experiment.getName() + "_visualized_trial" + (i + 1) + "_" + dateStr + ".mp4").toString();
                TrialResult trialResult = expResult.trialResults.get(i);
                VideoUtils.matsToVideo(trialResult.rawImages(), rawPath, getCore().fps);
                VideoUtils.renderedImagesToVideo(trialResult.visualizedImages(), visualizedPath, getCore().fps);
            }
        }
    }

    private void saveResultsTxtFile(String header, String date, String folderPath) {
        try {
            if (folderPath == null || folderPath.equals("Invalid File Path")) {
                System.out.println("Invalid save path when saving OMR results");
                return;
            }

            String name = header + "_results_" + date + ".txt";
            Path outputPath = Path.of(folderPath, name);

            StringBuilder builder = new StringBuilder();

            for (int expIndex = 0; expIndex < experimentResults.size(); expIndex++) {
                ExperimentResult result = experimentResults.get(expIndex);

                builder.append("========== EXPERIMENT ")
                        .append(expIndex + 1)
                        .append(" ==========\n");

                builder.append("Name: ").append(result.experiment.getName()).append("\n\n");

                builder.append("Ending Pattern:\n");
                builder.append("Speed: ").append(result.endingPattern.getSpeed()).append("\n");
                builder.append("Light Brightness: ").append(result.endingPattern.getLightBrightness()).append("\n");
                builder.append("Dark Brightness: ").append(result.endingPattern.getDarkBrightness()).append("\n");
                builder.append("Band Width: ").append(result.endingPattern.getBandWidth()).append("\n\n");

                builder.append("Trials Until Failure: ")
                        .append(result.getNumTrials() - 1)
                        .append("\n\n");

                builder.append("Trial Results:\n");

                for (int i = 0; i < result.getNumTrials(); i++) {
                    builder.append("Trial ").append(i + 1).append(":\n");
                    builder.append("  Num OMR Instances: ").append(result.getNumOMR(i)).append("\n");
                    builder.append("  Avg Duration (ms): ").append(result.getAverageDuration(i)).append("\n");
                    builder.append("  Median Duration (ms): ").append(result.getMedian(i)).append("\n\n");

                    // Add timestamps and head/tail angles
                    builder.append("Trial Head and Tail Angles (degrees)\n");
                    builder.append("timestamp (ms), head angle, tail angle\n");

                    TrialResult trial = result.trialResults.get(i);
                    ArrayList<Double> timestamps = trial.timestamps();
                    ArrayList<Double> headAngles = trial.headAngles();
                    ArrayList<Double> tailAngles = trial.tailAngles();

                    int n = Math.min(timestamps.size(), Math.min(headAngles.size(), tailAngles.size()));
                    for (int j = 0; j < n; j++) {
                        builder.append(timestamps.get(j))
                                .append(", ")
                                .append(headAngles.get(j))
                                .append(", ")
                                .append(tailAngles.get(j))
                                .append("\n");
                    }

                    builder.append("\n");
                }
            }

            Files.writeString(outputPath, builder.toString());
            System.out.println("Saved OMR results to: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void saveRawImages(String folder, ArrayList<Mat> images) {
        new Thread(() -> {
            for (int i = 0; i < images.size(); i++) {
                Imgcodecs.imwrite(Path.of(folder, i + ".png").toString(), images.get(i));
            }
            System.out.println("finished saving " + images.size() + " raw images");
            Platform.runLater(getCore().getStartMenuController()::updateButtonsEnabled);
        }, "save raw image thread").start();
    }

    private void saveVisualizedImages(String folderPath, ArrayList<RenderedImage> images) {
        new Thread(() -> {
            System.out.println("SAVING VISUALIZED IMAGES");
            for (int i=0; i<images.size(); i++) {
                // Write to file
                File file = Paths.get(folderPath, i + ".png").toFile();
                try {
                    ImageIO.write(images.get(i), "png", file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("finished saving " + images.size() + " annotated images");
            Platform.runLater(getCore().getStartMenuController()::updateButtonsEnabled);
        }, "save visualized images thread").start();
    }
}
