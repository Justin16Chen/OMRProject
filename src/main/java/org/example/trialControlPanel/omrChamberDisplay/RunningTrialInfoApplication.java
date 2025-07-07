package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.trialControlPanel.sceneManager.CustomApplication;
import org.example.trialControlPanel.sceneManager.SceneManager;

public class RunningTrialInfoApplication extends CustomApplication {
    public RunningTrialInfoApplication(SceneManager sceneManager) {
        super(sceneManager);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/RunningTrialInfo.fxml"));

            stage.setTitle("Current Trial Info");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            RunningTrialInfoController controller = loader.getController();
            controller.setSceneManager(getSceneManager());
            controller.setStage(stage);
            controller.setup();

        } catch(Exception e) {
            System.out.println("RUNNING TRIAL INFO APP FAILED TO LAUNCH");
        }
    }
}
