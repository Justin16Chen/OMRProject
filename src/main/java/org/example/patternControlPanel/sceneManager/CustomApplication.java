package org.example.patternControlPanel.sceneManager;

import javafx.application.Application;
import javafx.stage.Stage;

public abstract class CustomApplication extends Application {
    private final SceneManager sceneManager;
    private Stage stage;

    public CustomApplication(SceneManager sceneManager) {
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
    public void close() {
        stage.close();
    }
}
