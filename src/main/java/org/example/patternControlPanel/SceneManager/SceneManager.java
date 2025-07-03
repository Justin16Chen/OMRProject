package org.example.patternControlPanel.SceneManager;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.patternControlPanel.startMenu.StartMenuController;
import org.example.patternControlPanel.trialConfig.TrialConfigController;

public class SceneManager {

    public static FXMLLoader getLoaderFromResources(String filePath) {
        return new FXMLLoader(SceneManager.class.getResource(filePath));
    }

    private Stage primaryStage;
    private Scene startMenuScene;
    private Scene trialConfigScene;
    private StartMenuController startMenuController;
    private TrialConfigController trialConfigController;

    public void init(Stage primaryStage) {
        try {
            // load FXML and controllers
            FXMLLoader loader = getLoaderFromResources("/patternControlPanelFXML/StartMenu.fxml");
            startMenuScene = new Scene(loader.load());
            startMenuController = loader.getController();
            startMenuController.setSceneManager(this);

            FXMLLoader loader2 = getLoaderFromResources("/patternControlPanelFXML/TrialConfig.fxml");
            trialConfigScene = new Scene(loader2.load());
            trialConfigController = loader2.getController();
            startMenuController.setSceneManager(this);

            // setup primary stage
            this.primaryStage = primaryStage;
            primaryStage.setTitle("AutoOMR");
            primaryStage.setScene(startMenuScene);
            primaryStage.show();

        } catch(Exception e) {
            System.out.println("FAILED TO LOAD STAGE IN SCENE MANAGER");
            e.printStackTrace();
        }
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
    public Scene getTrialConfigScene() {
        return trialConfigScene;
    }
    public TrialConfigController getTrialConfigController() {
        return trialConfigController;
    }
}
