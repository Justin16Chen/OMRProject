package org.example.patternControlPanel.startMenu;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.patternControlPanel.sceneManager.CustomApplication;
import org.example.patternControlPanel.sceneManager.SceneManager;

public class RunTrialConfirmationApplication extends CustomApplication {

    public RunTrialConfirmationApplication(SceneManager sceneManager) {
        super(sceneManager);
    }

    @Override
    public void start(Stage stage) {
        try {
            setStage(stage);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/RunTrialConfirmation.fxml"));
            stage.setTitle("Start Menu");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            RunTrialConfirmationController controller = loader.getController();
            controller.setSceneManager(getSceneManager());
            controller.setStage(stage);

        } catch(Exception e) {
            System.out.println("QUEUE TRIAL APP FAILED TO LAUNCH");
            e.printStackTrace();
        }
    }
}
