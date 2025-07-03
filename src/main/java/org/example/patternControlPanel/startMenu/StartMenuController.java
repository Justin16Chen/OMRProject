package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.patternControlPanel.SceneManager.Controller;
import org.example.patternControlPanel.pattern.Pattern;
import org.example.patternControlPanel.pattern.PatternDirection;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.trialDataManager.TrialDataManager;

import java.util.ArrayList;
import java.util.List;

public class StartMenuController extends Controller {

    private ArrayList<TrialConfig> queuedTrials;

    public ArrayList<TrialConfig> getQueuedTrials() {
        return queuedTrials;
    }

    @FXML
    private void initialize() {
        queuedTrialsTextArea.setEditable(false);
        queuedTrials = new ArrayList<>(List.of(new TrialConfig("defaultTrial", new Pattern("", PatternDirection.CLOCKWISE, 1, 1, 1), 1, 1, 1, 1)));
        updateQueuedTrialsTextArea();
    }

    @FXML
    private Button createTrialButton;
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
    private Button queueTrialButton;
    @FXML
    private void handleQueueTrialButtonClick() {
        QueueTrialApplication queueTrialApplication = new QueueTrialApplication();
        queueTrialApplication.start(new Stage());
        queueTrialApplication.getController().setSceneManager(getSceneManager());
        queueTrialApplication.getController().updateSavedTrialsComboBox();
    }

    @FXML
    private Button clearQueuedTrialsButton;
    @FXML
    private void handleClearQueuedTrialsButtonClick() {
        queuedTrials.clear();
        updateQueuedTrialsTextArea();
    }
    @FXML
    private Button runQueueButton;
    @FXML
    private void handleRunQueueButtonClick() {

    }
}
