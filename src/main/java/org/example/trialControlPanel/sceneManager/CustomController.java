package org.example.trialControlPanel.sceneManager;

import javafx.stage.Stage;

public abstract class CustomController {
    private Stage stage;
    private SceneManager sceneManager;

    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }
    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public Stage getStage() {
        return stage;
    }

    // can be used to set up anything dependent on sceneManager
    public void setup() {}
}
