package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.trialConfig.TrialSaver;
import org.example.trialControlPanel.sceneManager.SceneManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main extends Application {
    private static final int cameraFPS = 30;

    public static void main(String[] args) {
        //startCameraTracking();
        launch(args);
        // runSSDEvaluations();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        TrialSaver.initializeTrialSaver();
        SceneManager sceneManager = new SceneManager();
        sceneManager.init(primaryStage);
    }

    private static void startCameraTracking() {
        Thread cameraThread = new Thread(() -> {
            double time = System.currentTimeMillis();
            double lastUpdateTime = time;
            double millisPerFrame = 1000.0 / cameraFPS;
            CameraManager cm = new CameraManager();

            while (true) {
                time = System.currentTimeMillis();
                if(time - lastUpdateTime > millisPerFrame) {
                    cm.update();
                    lastUpdateTime = time;
                }
            }
        });
        cameraThread.start();
    }

    private static void runSSDEvaluations() {
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
