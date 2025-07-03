package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.patternControlPanel.SceneManager.SceneManager;

public class Main extends Application{
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager sceneManager = new SceneManager();
        sceneManager.init(primaryStage);
    }
}
