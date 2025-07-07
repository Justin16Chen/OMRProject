package org.example.patternControlPanel.trialConfig;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.patternControlPanel.sceneManager.CustomApplication;
import org.example.patternControlPanel.sceneManager.SceneManager;
import org.example.patternControlPanel.startMenu.QueueTrialController;

public class SavedTrialsApplication extends CustomApplication {
    public SavedTrialsApplication(SceneManager sceneManager) {
        super(sceneManager);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/SavedTrials.fxml"));

            stage.setTitle("Saved Trials");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            SavedTrialsController controller = loader.getController();
            controller.setSceneManager(getSceneManager());
            controller.setStage(stage);

        } catch(Exception e) {
            System.out.println("SAVED TRIALS APP FAILED TO LAUNCH");
        }
    }

}
