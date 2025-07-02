package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.patternControlPanel.controlPanel.TrialConfigApplication;

import java.util.ArrayList;

public class StartMenuController {

    private ArrayList<String> queuedTrialNames;

    @FXML
    private void initialize() {
        queuedTrialNames = new ArrayList<>();
        queuedTrialsTextArea.setEditable(false);
        updateQueuedTrialsTextArea();
    }

    @FXML
    private Button createTrialButton;
    @FXML
    private void handleCreateTrialButtonClick() {
        new TrialConfigApplication().start(new Stage());
    }

    @FXML
    private TextArea queuedTrialsTextArea;
    public void updateQueuedTrialsTextArea() {
        for (int i=0; i<queuedTrialNames.size(); i++) {
            String newLine = i < queuedTrialNames.size() - 1 ? "\n" : "";
            queuedTrialsTextArea.setText(queuedTrialsTextArea.getText() + queuedTrialNames.get(i) + newLine);
        }
    }

    @FXML
    private Button queueTrialButton;
    @FXML
    private void handleQueueTrialButtonClick() {

    }

    @FXML
    private Button runQueueButton;
    @FXML
    private void handleRunQueueButtonClick() {

    }
}
