package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.patternControlPanel.trialConfig.TrialSaver;
import org.example.patternControlPanel.sceneManager.SceneManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
        // runSSDEvaluations();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        TrialSaver.initializeTrialSaver();
        SceneManager sceneManager = new SceneManager();
        sceneManager.init(primaryStage);
    }

    private void runSSDEvaluations() {
        String pythonPath = "C:\\Users\\justi\\anaconda3\\envs\\omrEnv\\python.exe";
        String scriptPath = "python\\omrPythonSide.py";
        ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
        try {
            Process process = pb.start();

            // outputting anything that the python script prints to console
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while((line = reader.readLine()) != null)
                System.out.println(line);

            // outputting any errors that occur in the python script
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while((line = errorReader.readLine()) != null)
                System.err.println("Error: " + line);

            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
