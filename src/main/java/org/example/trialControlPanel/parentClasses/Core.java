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
import org.example.trialControlPanel.omrChamberDisplay.ChildOMRController;
import org.example.trialControlPanel.startMenu.StartMenuController;
import org.example.trialControlPanel.trialConfig.TrialConfig;
import org.example.trialControlPanel.trialConfig.TrialConfigController;

import java.io.IOException;
import java.util.ArrayList;

public class Core {

    public static FXMLLoader getLoaderFromResources(String filePath) {
        return new FXMLLoader(Core.class.getResource(filePath));
    }

    public static final int NUM_OMR_CHAMBER_CHILDREN = 1;

    private ArrayList<Stage> stagesToClose;
    private Stage primaryStage;
    private Stage OMRChamberStage;
    private Stage[] childOMRChamberStages;
    private Stage runTrialStage;

    private Scene startMenuScene;
    private StartMenuController startMenuController;
    private ApplicationMonitorManager startMenuMonitorManager;
    private Scene trialConfigScene;
    private TrialConfigController trialConfigController;
    private Scene OMRChamberScene;
    private Scene[] childOMRChamberScenes;
    private OMRChamberController OMRChamberController;
    private ChildOMRController[] childOMRChamberControllers;
    private Scene runTrialScene;
    private RunTrialController runTrialController;

    private final CameraManager cameraManager;
    private final ProgramInfoManager programInfoManager;
    private final PythonRunner pythonRunner;

    public Core() {
        stagesToClose = new ArrayList<>();
        cameraManager = new CameraManager(0);
        cameraManager.clearRawImagesFolder();

        programInfoManager = new ProgramInfoManager();
        programInfoManager.startProgram();

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

    // any stages in the stagesToClose list will be closed when the primary stage closes
    public void addStageToClose(Stage stage) {
        stagesToClose.add(stage);
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

            primaryStage.setOnCloseRequest(e -> {
                programInfoManager.stopProgram();
                for (Stage stage : stagesToClose)
                    stage.close();
            });

            OMRChamberStage = new Stage();
            OMRChamberStage.setTitle("OMR Chamber");
            OMRChamberStage.initStyle(StageStyle.UNDECORATED);
            stagesToClose.add(OMRChamberStage);

            childOMRChamberStages = new Stage[NUM_OMR_CHAMBER_CHILDREN];
            for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++) {
                childOMRChamberStages[i] = new Stage();
                childOMRChamberStages[i].setTitle("OMR Chamber");
                childOMRChamberStages[i].initStyle(StageStyle.UNDECORATED);
                stagesToClose.add(childOMRChamberStages[i]);
            }
            childOMRChamberScenes = new Scene[NUM_OMR_CHAMBER_CHILDREN];
            childOMRChamberControllers = new ChildOMRController[NUM_OMR_CHAMBER_CHILDREN];

            runTrialStage = new Stage(); // stage to be shown when trials are running (shows info about current trial)
            runTrialStage.setTitle("Current Trial Info");
            stagesToClose.add(runTrialStage);

            // load FXML and controllers
            loadStartMenu();
            loadTrialConfig();
            loadOMRChamber();
            loadRunTrial();

            // setup stages
            primaryStage.setScene(startMenuScene);
            OMRChamberStage.setScene(OMRChamberScene);
            for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++)
                childOMRChamberStages[i].setScene(childOMRChamberScenes[i]);
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

    public void runOMRTrials(MonitorFormat[] chamberMonitorFormats, ArrayList<TrialConfig> trials, int restTime) {
        OMRChamberController.initPatternDrawer(chamberMonitorFormats[0], trials, restTime);
        Rectangle2D bounds = chamberMonitorFormats[0].getBounds();
        OMRChamberStage.setX(bounds.getMinX());
        OMRChamberStage.setY(bounds.getMinY());
        OMRChamberStage.setWidth(bounds.getWidth());
        OMRChamberStage.setHeight(bounds.getHeight());
        OMRChamberStage.show();
        OMRChamberController.resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());

        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++) {
            childOMRChamberControllers[i].initPatternDrawer(chamberMonitorFormats[i + 1], trials);
            bounds = chamberMonitorFormats[i + 1].getBounds();
            childOMRChamberStages[i].setX(bounds.getMinX());
            childOMRChamberStages[i].setY(bounds.getMinY());
            childOMRChamberStages[i].setWidth(bounds.getWidth());
            childOMRChamberStages[i].setHeight(bounds.getHeight());
            childOMRChamberStages[i].show();
            childOMRChamberControllers[i].resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());
        }

        runTrialController.setTrials(trials);
        runTrialController.updateUILabels();
        runTrialStage.setX(primaryStage.getX());
        runTrialStage.setY(primaryStage.getY());
        runTrialStage.show();

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

        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++) {
            FXMLLoader childLoader = getLoaderFromResources("/patternControlPanelFXML/ChildOMRChamber.fxml");
            childOMRChamberScenes[i] = new Scene(childLoader.load());
            childOMRChamberControllers[i] = childLoader.getController();
            childOMRChamberControllers[i].setCore(this);
            childOMRChamberControllers[i].setStage(childOMRChamberStages[i]);
        }
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
    public ChildOMRController[] getChildOMRControllers() {
        return childOMRChamberControllers;
    }
    public Scene getRunTrialScene() {
        return runTrialScene;
    }
    public RunTrialController getRunTrialController() {
        return runTrialController;
    }
}
