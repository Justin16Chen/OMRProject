package org.example.patternControlPanel.startMenu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RunTrialConfirmationApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/RunTrialConfirmation.fxml"));

            Parent root = loader.load();

            stage.setTitle("Start Menu");
            stage.setScene(new Scene(root));
            stage.show();

        } catch(Exception e) {
            System.out.println("QUEUE TRIAL APP FAILED TO LAUNCH");
        }
    }
}
