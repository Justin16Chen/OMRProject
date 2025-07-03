package org.example.patternControlPanel.SceneManager;

public abstract class Controller {
    private SceneManager sceneManager;

    public SceneManager getSceneManager() {
        if (sceneManager == null)
            throw new IllegalStateException("did not set scene manager for " + this);
        return sceneManager;
    }
    public void setSceneManager(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }
}
