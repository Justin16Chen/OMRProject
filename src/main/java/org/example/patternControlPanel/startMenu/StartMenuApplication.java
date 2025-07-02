package org.example.patternControlPanel.startMenu;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StartMenuApplication extends Application {
    @Override
    public void start(Stage stage) {
        try {
            // load FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/StartMenu.fxml"));

            // load parent node and create a scene
            Parent root = loader.load();

            // setup stage and scene
            stage.setTitle("Start Menu");
            stage.setScene(new Scene(root));
            stage.show();

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
