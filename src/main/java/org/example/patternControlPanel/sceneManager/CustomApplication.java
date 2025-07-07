package org.example.patternControlPanel.sceneManager;

import javafx.application.Application;
import javafx.stage.Stage;

public abstract class CustomApplication extends Application {
    private final SceneManager sceneManager;

    public CustomApplication(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }
}
