package org.example.patternControlPanel.sceneManager;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.patternControlPanel.omrChamberDisplay.OMRChamberController;
import org.example.patternControlPanel.pattern.MonitorFormat;
import org.example.patternControlPanel.startMenu.StartMenuController;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.trialConfig.TrialConfigController;

public class SceneManager {

    public static FXMLLoader getLoaderFromResources(String filePath) {
        return new FXMLLoader(SceneManager.class.getResource(filePath));
    }

    private Stage primaryStage;
    private ApplicationMonitorManager primaryStageMonitorManager;
    private Stage OMRChamberStage;

    private Scene startMenuScene;
    private StartMenuController startMenuController;
    private Scene trialConfigScene;
    private TrialConfigController trialConfigController;
    private Scene OMRChamberScene;
    private OMRChamberController OMRChamberController;

    public void init(Stage primaryStage) {
        try {
            // setup stages first so controllers don't get null pointers when calling getters
            this.primaryStage = primaryStage; // control panel stage
            primaryStage.setTitle("AutoOMR");
            primaryStageMonitorManager = new ApplicationMonitorManager(primaryStage);

            OMRChamberStage = new Stage(); // stage to be shown in OMR chamber
            OMRChamberStage.setTitle("OMR Chamber");
            OMRChamberStage.initStyle(StageStyle.UNDECORATED);

            // load FXML and controllers
            FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/StartMenu.fxml");
            startMenuScene = new Scene(loader.load());
            startMenuController = loader.getController();
            startMenuController.setSceneManager(this);
            startMenuController.setup();
            primaryStageMonitorManager.setMonitorFormatConsumer(mf -> startMenuController.setStartMenuMonitorFormat(mf));

            FXMLLoader loader2 = getLoaderFromResources("/patternControlPanelFXML/TrialConfig.fxml");
            trialConfigScene = new Scene(loader2.load());
            trialConfigController = loader2.getController();
            trialConfigController.setSceneManager(this);
            trialConfigController.setup();

            FXMLLoader loader3 = getLoaderFromResources("/patternControlPanelFXML/Display.fxml");
            OMRChamberScene = new Scene(loader3.load());
            OMRChamberController = loader3.getController();
            OMRChamberController.setSceneManager(this);
            OMRChamberController.setStage(OMRChamberStage);
            OMRChamberController.setup();

            // show primary stage
            primaryStage.setScene(startMenuScene);
            primaryStage.show();

        } catch(Exception e) {
            System.out.println("FAILED TO LOAD STAGE IN SCENE MANAGER");
            e.printStackTrace();
        }
    }

    public void setupOMRChamberStage(MonitorFormat monitorFormat, TrialConfig trialConfig) {
        OMRChamberController.setMonitorFormat(monitorFormat);
        OMRChamberController.setTrialConfig(trialConfig);
        OMRChamberController.initPatternDrawer();
        OMRChamberController.startTrials();
        Rectangle2D bounds = monitorFormat.getBounds();
        OMRChamberStage.setScene(OMRChamberScene);
        OMRChamberStage.setX(bounds.getMinX());
        OMRChamberStage.setY(bounds.getMinY());
        OMRChamberStage.setWidth(bounds.getWidth());
        OMRChamberStage.setHeight(bounds.getHeight());
        OMRChamberController.resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());
        OMRChamberStage.show();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
    public Stage getOMRChamberStage() { return OMRChamberStage; }

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
    public Scene getOMRChamberScene() { return OMRChamberScene; }
    public OMRChamberController getOMRChamberController() { return OMRChamberController; }
}
