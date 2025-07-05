package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.patternControlPanel.sceneManager.CustomApplication;
import org.example.patternControlPanel.sceneManager.SceneManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager sceneManager = new SceneManager();
        sceneManager.init(primaryStage);
    }
}
