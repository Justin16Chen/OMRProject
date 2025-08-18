package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.trialConfig.TrialSaver;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final Core core;

    public Main() {
        core = new Core();
    }

    @Override
    public void start(Stage primaryStage) throws IOException, InterruptedException {
        TrialSaver.initializeTrialSaver();
        core.init(primaryStage);
    }

}
