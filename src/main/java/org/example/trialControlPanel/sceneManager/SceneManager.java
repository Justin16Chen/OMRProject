package org.example.trialControlPanel.sceneManager;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.trialControlPanel.monitorInfo.ApplicationMonitorManager;
import org.example.trialControlPanel.omrChamberDisplay.OMRChamberController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.omrChamberDisplay.RunTrialController;
import org.example.trialControlPanel.startMenu.StartMenuController;
import org.example.trialControlPanel.trialConfig.TrialConfig;
import org.example.trialControlPanel.trialConfig.TrialConfigController;

public class SceneManager {

    public static FXMLLoader getLoaderFromResources(String filePath) {
        return new FXMLLoader(SceneManager.class.getResource(filePath));
    }

    private Stage primaryStage;
    private Stage OMRChamberStage;
    private Stage runTrialStage;

    private Scene startMenuScene;
    private StartMenuController startMenuController;
    private Scene trialConfigScene;
    private TrialConfigController trialConfigController;
    private Scene OMRChamberScene;
    private OMRChamberController OMRChamberController;
    private Scene runTrialScene;
    private RunTrialController runTrialController;

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
            FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/StartMenu.fxml");
            startMenuScene = new Scene(loader.load());
            startMenuController = loader.getController();
            startMenuController.setSceneManager(this);
            startMenuController.setStage(primaryStage);

            FXMLLoader loader2 = getLoaderFromResources("/patternControlPanelFXML/TrialConfig.fxml");
            trialConfigScene = new Scene(loader2.load());
            trialConfigController = loader2.getController();
            trialConfigController.setSceneManager(this);
            trialConfigController.setStage(primaryStage);

            FXMLLoader loader3 = getLoaderFromResources("/patternControlPanelFXML/OMRChamber.fxml");
            OMRChamberScene = new Scene(loader3.load());
            OMRChamberController = loader3.getController();
            OMRChamberController.setSceneManager(this);
            OMRChamberController.setStage(OMRChamberStage);

            FXMLLoader loader4 = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/RunningTrialInfo.fxml"));
            runTrialScene = new Scene(loader4.load());
            runTrialController = loader4.getController();
            runTrialController.setSceneManager(this);
            runTrialController.setStage(runTrialStage);

            // setup stages
            primaryStage.setScene(startMenuScene);
            OMRChamberStage.setScene(OMRChamberScene);
            runTrialStage.setScene(runTrialScene);

            primaryStage.show();

            // setup primary stage monitor format
            new ApplicationMonitorManager(primaryStage, mf -> startMenuController.setStartMenuMonitorFormat(mf));

            // setup controllers last (to avoid null pointer exceptions)
            startMenuController.setup();
            trialConfigController.setup();
            OMRChamberController.setup();
            runTrialController.setup();

        } catch(Exception e) {
            System.out.println("FAILED TO LOAD STAGE IN SCENE MANAGER");
            e.printStackTrace();
        }
    }

    public void runOMRTrials(MonitorFormat chamberMonitorFormat, TrialConfig trialConfig) {
        OMRChamberController.initPatternDrawer(chamberMonitorFormat, trialConfig);
        Rectangle2D bounds = chamberMonitorFormat.getBounds();
        OMRChamberStage.setX(bounds.getMinX());
        OMRChamberStage.setY(bounds.getMinY());
        OMRChamberStage.setWidth(bounds.getWidth());
        OMRChamberStage.setHeight(bounds.getHeight());
        OMRChamberController.resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());
        OMRChamberStage.show();

        runTrialController.setTrial(trialConfig);
        runTrialController.updateUI();
        getRunTrialStage().show();

        OMRChamberController.startTrials();
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
