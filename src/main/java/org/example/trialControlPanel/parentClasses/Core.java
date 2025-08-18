package org.example.trialControlPanel.parentClasses;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.integration.ProgramInfoManager;
import org.example.integration.PythonRunner;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.monitorInfo.ApplicationMonitorManager;
import org.example.trialControlPanel.omrChamberDisplay.OMRChamberController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.omrChamberDisplay.RunTrialController;
import org.example.trialControlPanel.startMenu.StartMenuController;
import org.example.trialControlPanel.trialConfig.TrialConfig;
import org.example.trialControlPanel.trialConfig.TrialConfigController;

import java.io.IOException;
import java.util.ArrayList;

public class Core {

    public static FXMLLoader getLoaderFromResources(String filePath) {
        return new FXMLLoader(Core.class.getResource(filePath));
    }

    private Stage primaryStage;
    private Stage OMRChamberStage;
    private Stage runTrialStage;

    private Scene startMenuScene;
    private StartMenuController startMenuController;
    private ApplicationMonitorManager startMenuMonitorManager;
    private Scene trialConfigScene;
    private TrialConfigController trialConfigController;
    private Scene OMRChamberScene;
    private OMRChamberController OMRChamberController;
    private Scene runTrialScene;
    private RunTrialController runTrialController;

    private final CameraManager cameraManager;
    private final ProgramInfoManager programInfoManager;
    private final PythonRunner pythonRunner;

    public Core() {
        cameraManager = new CameraManager(0);
        cameraManager.clearImageFolder();

        programInfoManager = new ProgramInfoManager();
        programInfoManager.deactivateTrial();

        pythonRunner = new PythonRunner();

        Thread pythonThread = new Thread(() -> {
            try {
                pythonRunner.start();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        pythonThread.start();
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }
    public ProgramInfoManager getProgramInfoWriter() {
        return programInfoManager;
    }

    public void init(Stage primaryStage) {
        try {
            // setup stages first so controllers don't get null pointers when calling getters
            this.primaryStage = primaryStage; // control panel stage
            primaryStage.setTitle("AutoOMR");

            OMRChamberStage = new Stage(); // stage to be shown in OMR chamber
            OMRChamberStage.setTitle("OMR Chamber");
            OMRChamberStage.initStyle(StageStyle.UNDECORATED);

            runTrialStage = new Stage(); // stage to be shown when trials are running (shows info about current trial)
            runTrialStage.setTitle("Current Trial Info");

            // load FXML and controllers
            loadStartMenu();
            loadTrialConfig();
            loadOMRChamber();
            loadRunTrial();

            // setup stages
            primaryStage.setScene(startMenuScene);
            OMRChamberStage.setScene(OMRChamberScene);
            runTrialStage.setScene(runTrialScene);

            primaryStage.setResizable(false);
            primaryStage.show();

            // setup primary stage monitor format
            startMenuMonitorManager = new ApplicationMonitorManager(primaryStage, mf -> startMenuController.setStartMenuMonitorFormat(mf));

            // setup controllers last (to avoid null pointer exceptions)
            startMenuController.setup();
            trialConfigController.setup();
            OMRChamberController.setup();
            runTrialController.setup();

        } catch(Exception e) {
            System.out.println("FAILED TO LOAD STAGES/SCENES/CONTROLLERS IN CORE");
            e.printStackTrace();
        }
    }

    public void runOMRTrials(MonitorFormat chamberMonitorFormat, ArrayList<TrialConfig> trials, int restTime) {
        OMRChamberController.initPatternDrawer(chamberMonitorFormat, trials, restTime);
        Rectangle2D bounds = chamberMonitorFormat.getBounds();
        OMRChamberStage.setX(bounds.getMinX());
        OMRChamberStage.setY(bounds.getMinY());
        OMRChamberStage.setWidth(bounds.getWidth());
        OMRChamberStage.setHeight(bounds.getHeight());
        OMRChamberController.resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());
        OMRChamberStage.show();

        runTrialController.setTrials(trials);
        runTrialController.updateUILabels();
        getRunTrialStage().show();

        OMRChamberController.startTrials();
    }

    public void loadStartMenu() throws IOException {
        FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/StartMenu.fxml");
        startMenuScene = new Scene(loader.load());
        startMenuController = loader.getController();
        startMenuController.setCore(this);
        startMenuController.setStage(primaryStage);
    }

    public void loadTrialConfig() throws IOException {
        FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/TrialConfig.fxml");
        trialConfigScene = new Scene(loader.load());
        trialConfigController = loader.getController();
        trialConfigController.setCore(this);
        trialConfigController.setStage(primaryStage);
    }

    public void loadOMRChamber() throws IOException {
        FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/OMRChamber.fxml");
        OMRChamberScene = new Scene(loader.load());
        OMRChamberController = loader.getController();
        OMRChamberController.setCore(this);
        OMRChamberController.setStage(OMRChamberStage);
    }

    public void loadRunTrial() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/RunningTrialInfo.fxml"));
        runTrialScene = new Scene(loader.load());
        runTrialController = loader.getController();
        runTrialController.setCore(this);
        runTrialController.setStage(runTrialStage);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
    public Stage getOMRChamberStage() {
        return OMRChamberStage;
    }
    public Stage getRunTrialStage() {
        return runTrialStage;
    }

    public Scene getStartMenuScene() {
        return startMenuScene;
    }
    public StartMenuController getStartMenuController() {
        return startMenuController;
    }
    public ApplicationMonitorManager getStartMenuMonitorManager() { return startMenuMonitorManager; }
    public Scene getTrialConfigScene() {
        return trialConfigScene;
    }
    public TrialConfigController getTrialConfigController() {
        return trialConfigController;
    }
    public Scene getOMRChamberScene() {
        return OMRChamberScene;
    }
    public OMRChamberController getOMRChamberController() {
        return OMRChamberController;
    }
    public Scene getRunTrialScene() {
        return runTrialScene;
    }
    public RunTrialController getRunTrialController() {
        return runTrialController;
    }
}
