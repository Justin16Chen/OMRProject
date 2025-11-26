package org.example.trialControlPanel.omrResults;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.pattern.Pattern;
import org.example.trialControlPanel.trialConfig.Experiment;
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

public class ResultsController extends CustomController {
    // for each experiment, a list of trial names and omr results
    // this list is automatically cleared everytime core.runOMRTrials() is called b/c it reloads the scene and controller from the FXML file
    private final ArrayList<ExperimentResult> experimentResults = new ArrayList<>();
    private ExperimentResult curExperimentResult;
    public boolean earlyStop = false;
    @Override
    public void setup() {
        experimentResultsTextArea.setEditable(false);
        currentExperimentIndex = 0;
        updateButtonsEnabled();

        if (!earlyStop)
            updateUIToNewExperiment();

//        experimentNameTextField.textProperty().addListener((o, n, obs) -> {
//            rawVideoLabel.setText(experimentNameTextField.getText() + "_raw_");
//        });
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
    private TextField experimentNameTextField, saveFilePathTextField;
    @FXML
    private Label rawVideoLabel, visualizedVideoLabel, resultsFileLabel;

    @FXML
    private void handleChooseFilePathClick() {
        String path = promptUserForFolderPath("D:/OMR");
        updateSavePath(path);
    }
    @FXML
    private Button saveImagesButton;
    @FXML
    private void handleSaveImagesClick() {
        String folderPath = saveFilePathTextField.getText();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        saveResultsTxtFile(folderPath, dateStr);
        for (ExperimentResult expResult : experimentResults)
            for (int i=0; i<expResult.trialResults.size(); i++) {
                String rawPath = Path.of(folderPath, experimentNameTextField.getText() + "_" + expResult.experiment.getName() + "_raw_trial" + (i + 1) + "_" + dateStr + ".mp4").toString();
                String visualizedPath = Path.of(folderPath, experimentNameTextField.getText() + "_" + expResult.experiment.getName() + "_visualized_trial" + (i + 1) + "_" + dateStr + ".mp4").toString();
                TrialResult trialResult = expResult.trialResults.get(i);
                VideoUtils.matsToVideo(trialResult.rawImages(), rawPath, getCore().fps);
                VideoUtils.renderedImagesToVideo(trialResult.visualizedImages(), visualizedPath, getCore().fps);
            }
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

    private void saveResultsTxtFile(String folderPath, String dateStr) {
        try {
            if (folderPath == null || folderPath.equals("Invalid File Path")) {
                System.out.println("Invalid save path when saving OMR results");
                return;
            }

            String name = experimentNameTextField.getText() + "_" + dateStr + "_results.txt";
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

    public void saveRawImages(String folder, ArrayList<Mat> images) {
        new Thread(() -> {
            for (int i = 0; i < images.size(); i++) {
                Imgcodecs.imwrite(Path.of(folder, i + ".png").toString(), images.get(i));
            }
            System.out.println("finished saving " + images.size() + " raw images");
            Platform.runLater(getCore().getStartMenuController()::updateButtonsEnabled);
        }, "save raw image thread").start();
    }

    public void saveVisualizedImages(String folderPath, ArrayList<RenderedImage> images) {
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
