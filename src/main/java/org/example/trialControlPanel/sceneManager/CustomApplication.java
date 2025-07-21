package org.example.trialControlPanel.sceneManager;

import javafx.application.Application;

public abstract class CustomApplication extends Application {
    private final Core core;

    public CustomApplication(Core core) {
        this.core = core;
    }

    public Core getCore() {
        return core;
    }
}
