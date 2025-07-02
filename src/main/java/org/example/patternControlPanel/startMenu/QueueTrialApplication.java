package org.example.patternControlPanel.startMenu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.patternControlPanel.trialConfig.TrialConfig;

import java.util.ArrayList;

public class QueueTrialApplication extends Application {
    private QueueTrialController controller;
    private Stage stage;
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/QueueTrial.fxml"));

            Parent root = loader.load();
            controller = loader.getController();
            controller.setApplication(this);

            this.stage = stage;
            stage.setTitle("Start Menu");
            stage.setScene(new Scene(root));
            stage.show();

        } catch(Exception e) {
            System.out.println("QUEUE TRIAL APP FAILED TO LAUNCH");
        }
    }


    public QueueTrialController getController() {
        return controller;
    }

    public void closeApplication() {
        stage.close();
    }
}
