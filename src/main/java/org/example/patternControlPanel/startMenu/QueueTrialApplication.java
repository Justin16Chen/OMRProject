package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.patternControlPanel.sceneManager.CustomApplication;
import org.example.patternControlPanel.sceneManager.SceneManager;

public class QueueTrialApplication extends CustomApplication {
    private Stage stage;

    public QueueTrialApplication(SceneManager sceneManager) {
        super(sceneManager);
    }

    @Override
    public void start(Stage stage) {
        try {
            this.stage = stage;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/QueueTrial.fxml"));

            stage.setTitle("Start Menu");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            QueueTrialController controller = loader.getController();
            controller.setStage(stage);
            controller.updateSavedTrialsComboBox();

        } catch(Exception e) {
            System.out.println("QUEUE TRIAL APP FAILED TO LAUNCH");
        }
    }

    public void close() {
        stage.close();
    }
}
