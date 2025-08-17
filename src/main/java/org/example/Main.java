package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.sceneManager.Core;
import org.example.trialControlPanel.trialConfig.TrialSaver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final Core core;

    public Main() {
        core = new Core();
    }

    @Override
    public void start(Stage primaryStage) {
        TrialSaver.initializeTrialSaver();
        core.init(primaryStage);

    }

}
