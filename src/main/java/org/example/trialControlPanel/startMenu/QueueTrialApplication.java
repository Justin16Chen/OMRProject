package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.trialControlPanel.sceneManager.CustomApplication;
import org.example.trialControlPanel.sceneManager.SceneManager;

public class QueueTrialApplication extends CustomApplication {

    public QueueTrialApplication(SceneManager sceneManager) {
        super(sceneManager);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/QueueTrial.fxml"));

            stage.setTitle("Start Menu");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            QueueTrialController controller = loader.getController();
            controller.setSceneManager(getSceneManager());
            controller.setStage(stage);
            controller.updateSavedTrialsComboBox();

        } catch(Exception e) {
            System.out.println("QUEUE TRIAL APP FAILED TO LAUNCH");
        }
    }
}
