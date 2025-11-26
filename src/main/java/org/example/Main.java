package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.trialConfig.TrialSaver;

import java.io.IOException;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);

//        try {
//            Thread.sleep(100);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        System.out.println("JAVAFX DONE: RUNNING THREADS: ");
//        for (Thread thread : Thread.getAllStackTraces().keySet())
//            System.out.println(thread.getName());
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
