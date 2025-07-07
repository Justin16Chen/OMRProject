package org.example.trialControlPanel.sceneManager;

import javafx.application.Application;

public abstract class CustomApplication extends Application {
    private final SceneManager sceneManager;

    public CustomApplication(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }
}
