package org.example.trialControlPanel.omrResults;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.pattern.Pattern;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class ResultsController extends CustomController {
    // for each experiment, a list of trial names and omr results
    // this list is automatically cleared everytime core.runOMRTrials() is called b/c it reloads the scene and controller from the FXML file
    private final ArrayList<ExperimentResult> experimentResults = new ArrayList<>();
    private final ArrayList<double[]> curExperimentResults = new ArrayList<>();
    public boolean earlyStop = false;
    public void addTrialResult(double[] results) {
        curExperimentResults.add(results);
    }
    public void finishExperiment(Experiment experiment, Pattern endingPattern) {
        System.out.println("adding " + experiment);
        System.out.println("ending pattern: " + endingPattern);
        ArrayList<double[]> copiedExperimentResults = new ArrayList<>();
        for (double[] data : curExperimentResults) {
            double[] newData = new double[data.length];
            System.arraycopy(data, 0, newData, 0, data.length);
            copiedExperimentResults.add(newData);

        }
        ExperimentResult result = new ExperimentResult(experiment, copiedExperimentResults, endingPattern);
        experimentResults.add(result);
        curExperimentResults.clear();
    }
    @Override
    public void setup() {
        experimentResultsTextArea.setEditable(false);
        currentExperimentIndex = 0;
        updateButtonsEnabled();

        if (!earlyStop)
            updateUIToNewExperiment();
    }

    @FXML
    private Label experimentNameLabel;
    @FXML
    private TextArea experimentResultsTextArea;
    @FXML
    private Label numTrialsUntilFailLabel;

    @FXML
    private Label speedLabel;
    @FXML
    private Label lightBandBrightnessLabel;
    @FXML
    private Label darkBandBrightnessLabel;
    @FXML
    private Label bandWidthLabel;
    @FXML
    private TextField saveFilePathTextField;
    private boolean hasInvalidFilePath;

    @FXML
    private void handleChooseFilePathClick() {
        String path = promptUserForEmptyFolderPath(null);
        updateSavePath(path);
    }
    @FXML
    private Button saveImagesButton;
    @FXML
    private void handleSaveImagesClick() {
        saveResults();
    }

    private int currentExperimentIndex;
    @FXML
    private Label curExperimentShownLabel;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private void handlePrevExperimentClick() {
        currentExperimentIndex--;
        updateButtonsEnabled();
        updateUIToNewExperiment();
    }
    @FXML
    private void handleNextExperimentClick() {
        currentExperimentIndex++;
        updateButtonsEnabled();
        updateUIToNewExperiment();
    }

    private void updateUIToNewExperiment() {
        ExperimentResult result = experimentResults.get(currentExperimentIndex);
        experimentNameLabel.setText("Name: " + result.experiment.getName());

        experimentResultsTextArea.setText("");
        for (int i=0; i<result.getNumTrials(); i++) {
            String textToAdd =  "Trial " + (i + 1) + ": " + result.getNumOMR(i);
            experimentResultsTextArea.setText(experimentResultsTextArea.getText() + textToAdd + "\n");
        }

        numTrialsUntilFailLabel.setText("Trials Until Failure: " + (result.getNumTrials() - 1));

        speedLabel.setText("" + result.endingPattern.getSpeed());
        lightBandBrightnessLabel.setText("" + result.endingPattern.getLightBrightness());
        darkBandBrightnessLabel.setText("" + result.endingPattern.getDarkBrightness());
        bandWidthLabel.setText("" + result.endingPattern.getBandWidth());

        curExperimentShownLabel.setText("Viewing experiment " + (currentExperimentIndex + 1) + "/" + experimentResults.size());
    }

    private void updateButtonsEnabled() {
        prevButton.setDisable(currentExperimentIndex == 0);
        nextButton.setDisable(currentExperimentIndex == experimentResults.size() - 1);
    }
    private void updateSavePath(String path) {
        boolean validPath = Files.exists(Path.of(path));
        saveImagesButton.setDisable(!validPath);
        saveFilePathTextField.setText(validPath ? path : "Invalid File Path");
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

    private void saveResults() {
        try {
            String folderPath = saveFilePathTextField.getText();
            if (folderPath == null || folderPath.equals("Invalid File Path")) {
                System.out.println("Invalid save path when saving OMR results");
                return;
            }

            Path outputPath = Path.of(folderPath, "omrResults.txt");

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

                builder.append("Trial Results:\n");

                for (int i = 0; i < result.getNumTrials(); i++) {
                    builder.append("Trial ").append(i + 1).append(":\n");
                    builder.append("  Num OMR Instances: ").append(result.getNumOMR(i)).append("\n");
                    builder.append("  Avg Duration (ms): ").append(result.getAverageDuration(i)).append("\n");
                    builder.append("  Median Duration (ms): ").append(result.getMedian(i)).append("\n");
                    builder.append("\n");
                }

                builder.append("Trials Until Failure: ")
                        .append(result.getNumTrials() - 1)
                        .append("\n\n");
            }

            Files.writeString(outputPath, builder.toString());
            System.out.println("Saved OMR results to: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
