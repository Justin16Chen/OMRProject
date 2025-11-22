package org.example.trialControlPanel.parentClasses;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.integration.JsonManager;
import org.example.integration.SocketManager;
import org.example.integration.PythonRunner;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.monitorInfo.ApplicationMonitorManager;
import org.example.trialControlPanel.omrChamberDisplay.OMRChamberController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.omrChamberDisplay.RunTrialController;
import org.example.trialControlPanel.omrChamberDisplay.ChildOMRController;
import org.example.trialControlPanel.startMenu.LoadingController;
import org.example.trialControlPanel.startMenu.StartMenuController;
import org.example.trialControlPanel.trialConfig.Experiment;
import org.example.trialControlPanel.trialConfig.TrialConfigController;

import java.io.IOException;
import java.util.ArrayList;

public class Core {

    public static FXMLLoader getLoaderFromResources(String filePath) {
        return new FXMLLoader(Core.class.getResource(filePath));
    }

    public static final int NUM_OMR_CHAMBER_CHILDREN = 1;

    private Thread loadBgThread;
    private final ArrayList<Stage> stagesToClose;
    private Stage primaryStage;
    private Stage omrChamberStage;
    private Stage[] childOMRChamberStages;
    private Stage runTrialStage;

    private Scene loadingScene;
    private LoadingController loadingController;
    private Scene startMenuScene;
    private StartMenuController startMenuController;
    private ApplicationMonitorManager startMenuMonitorManager;
    private Scene trialConfigScene;
    private TrialConfigController trialConfigController;
    private Scene omrChamberScene;
    private Scene[] childOMRChamberScenes;
    private OMRChamberController omrChamberController;
    private ChildOMRController[] childOMRChamberControllers;
    private Scene runTrialScene;
    private RunTrialController runTrialController;

    private final CameraManager cameraManager;
    private final SocketManager socketManager;
    private final JsonManager jsonManager;
    private final PythonRunner pythonRunner;
    public final int fps = 24;

    public Core() {
        stagesToClose = new ArrayList<>();

        socketManager = new SocketManager();
        jsonManager = new JsonManager();
        cameraManager = new CameraManager(0, this);
        pythonRunner = new PythonRunner();

        pythonRunner.start();
    }

    // any stages in the stagesToClose list will be closed when the primary stage closes
    public void addStageToClose(Stage stage) {
        stagesToClose.add(stage);
    }
    public CameraManager getCameraManager() {
        return cameraManager;
    }
    public JsonManager getJsonManager() {
        return jsonManager;
    }
    public SocketManager getSocketManager() {
        return socketManager;
    }

    private void closeApplication() {
        System.out.println("CLOSING APPLICATION");
        socketManager.stop();
        loadBgThread.interrupt();
        for (Stage stage : stagesToClose)
            stage.close();
        cameraManager.stopEverything();
        pythonRunner.stopRunning();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("RUNNING THREADS: ");
        for (Thread thread : Thread.getAllStackTraces().keySet())
            System.out.println(thread.getName());
    }

    public void init(Stage primaryStage) {
        // load only the necessities
        try {
            // setup stages first so controllers don't get null pointers when calling getters
            this.primaryStage = primaryStage; // control panel stage
            primaryStage.setTitle("AutoOMR");
            primaryStage.setResizable(false);

            primaryStage.setOnCloseRequest(e -> {
                closeApplication();
            });

            // load FXML and controllers
            loadLoadingScreen();
            loadingController.setup();
            primaryStage.setScene(loadingScene);
            primaryStage.show();

            // load heavy stuff in background thread to not block JavaFX thread
            loadBgThread = new Thread(() -> {
                finishInitializingBackgroundWork();

                // setup controllers last (to avoid null pointer exceptions)
                Platform.runLater(() -> {
                    startMenuController.setup();
                    trialConfigController.setup();
                    omrChamberController.setup();
                    primaryStage.setScene(startMenuScene);

                    // connect input socket
                    omrChamberController.connectVisualizedImageInputSocket();
                });
            }, "load bg work on start");
            loadBgThread.start();
        } catch(Exception e) {
            System.out.println("FAILED TO LOAD STAGES/SCENES/CONTROLLERS IN CORE");
            e.printStackTrace();
        }
    }

    // want to first load loading scene, then load everything else
    private void finishInitializingBackgroundWork() {
        try {
            // load FXML and controllers
            loadStartMenu();
            loadTrialConfig();
            loadOMRChamberScenesAndControllers();

            // setup primary stage monitor format
            startMenuMonitorManager = new ApplicationMonitorManager(primaryStage, mf -> startMenuController.setStartMenuMonitorFormat(mf));

            if (Thread.currentThread().isInterrupted())
                return;

            // connect output socket
            try {
                System.out.println("before connecting socket");
                socketManager.connectOutputStream();

                if (Thread.currentThread().isInterrupted())
                    return;

                socketManager.writeHeaderData(cameraManager.getFrameWidth(), cameraManager.getFrameHeight(), fps);
                System.out.println("after connecting socket");
            } catch (IOException e) {
                System.out.println("failed to connect socketManager.outputStream/failed to write header data");
                throw new RuntimeException(e);
            }
        } catch(Exception e) {
            System.out.println("FAILED TO LOAD STAGES/SCENES/CONTROLLERS IN CORE");
            e.printStackTrace();
        }
    }

    private void removeStageFromCloseList(Stage stageToRemove) {
        if (stageToRemove == null)
            return;
        for (Stage stage : stagesToClose)
            if (stage == stageToRemove) {
                stagesToClose.remove(stageToRemove);
                return;
            }
    }
    public void runOMRTrials(MonitorFormat[] chamberMonitorFormats, ArrayList<Experiment> experiments, int restTime) {
        try {
            loadOMRChamberEverything(chamberMonitorFormats, experiments);
            loadRunTrialEverything();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        omrChamberController.setupAndStartExperiments(chamberMonitorFormats[0], experiments, restTime);
    }
    public void loadLoadingScreen() throws IOException {
        FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/Loading.fxml");
        loadingScene = new Scene(loader.load());
        loadingController = loader.getController();
        loadingController.setCore(this);
        loadingController.setStage(primaryStage);

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

    public void loadOMRChamberScenesAndControllers() throws IOException {
        // load scenes and controllers
        childOMRChamberScenes = new Scene[NUM_OMR_CHAMBER_CHILDREN];
        childOMRChamberControllers = new ChildOMRController[NUM_OMR_CHAMBER_CHILDREN];

        FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/OMRChamber.fxml");
        omrChamberScene = new Scene(loader.load());
        omrChamberController = loader.getController();
        omrChamberController.setCore(this);

        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++) {
            FXMLLoader childLoader = getLoaderFromResources("/patternControlPanelFXML/ChildOMRChamber.fxml");
            childOMRChamberScenes[i] = new Scene(childLoader.load());
            childOMRChamberControllers[i] = childLoader.getController();
            childOMRChamberControllers[i].setCore(this);
        }
    }
    public void loadOMRChamberEverything(MonitorFormat[] chamberMonitorFormats, ArrayList<Experiment> experiments) {
        Rectangle2D bounds = chamberMonitorFormats[0].getBounds();

        removeStageFromCloseList(omrChamberStage);

        // load stages
        omrChamberStage = new Stage();
        omrChamberStage.setTitle("OMR Chamber");
        omrChamberStage.initStyle(StageStyle.UNDECORATED);
        stagesToClose.add(omrChamberStage);

        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++)
            removeStageFromCloseList(childOMRChamberStages[i]);
        childOMRChamberStages = new Stage[NUM_OMR_CHAMBER_CHILDREN];
        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++) {
            childOMRChamberStages[i] = new Stage();
            childOMRChamberStages[i].setTitle("OMR Chamber");
            childOMRChamberStages[i].initStyle(StageStyle.UNDECORATED);
            stagesToClose.add(childOMRChamberStages[i]);
        }

        // set stage scenes
        omrChamberStage.setScene(omrChamberScene);
        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++)
            childOMRChamberStages[i].setScene(childOMRChamberScenes[i]);

        // update controllers
        omrChamberController.setStage(omrChamberStage);
        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++)
            childOMRChamberControllers[i].setStage(childOMRChamberStages[i]);

        // position stages
        omrChamberStage.setX(bounds.getMinX());
        omrChamberStage.setY(bounds.getMinY());
        omrChamberStage.setWidth(bounds.getWidth());
        omrChamberStage.setHeight(bounds.getHeight());
        omrChamberStage.show();
        omrChamberController.resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());
        for (int i=0; i<NUM_OMR_CHAMBER_CHILDREN; i++) {
            childOMRChamberControllers[i].initPatternDrawer(chamberMonitorFormats[i + 1], experiments);
            bounds = chamberMonitorFormats[i + 1].getBounds();
            childOMRChamberStages[i].setX(bounds.getMinX());
            childOMRChamberStages[i].setY(bounds.getMinY());
            childOMRChamberStages[i].setWidth(bounds.getWidth());
            childOMRChamberStages[i].setHeight(bounds.getHeight());
            childOMRChamberStages[i].show();
            childOMRChamberControllers[i].resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());
        }

    }
    public void loadRunTrialEverything() throws IOException {
        // remove previous runTrialStage
        for (Stage stage : stagesToClose)
            if (stage == runTrialStage) {
                stagesToClose.remove(runTrialStage);
                break;
            }

        runTrialStage = new Stage();
        runTrialStage.setTitle("Current Trial Info");
        runTrialStage.show();
        stagesToClose.add(runTrialStage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/RunningTrialInfo.fxml"));
        runTrialScene = new Scene(loader.load());
        runTrialController = loader.getController();
        runTrialController.setCore(this);
        runTrialController.setStage(runTrialStage);
        runTrialStage.setScene(runTrialScene);
        runTrialController.setup();

        runTrialController.setDisplaySM(omrChamberController.getDisplaySM());
        runTrialStage.setX(primaryStage.getX());
        runTrialStage.setY(primaryStage.getY());
    }

    public Stage getPrimaryStage() {
        return primaryStage;
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
    public Scene getOmrChamberScene() {
        return omrChamberScene;
    }
    public OMRChamberController getOmrChamberController() {
        return omrChamberController;
    }
    public ChildOMRController[] getChildOMRControllers() {
        return childOMRChamberControllers;
    }
    public RunTrialController getRunTrialController() {
        return runTrialController;
    }
}
