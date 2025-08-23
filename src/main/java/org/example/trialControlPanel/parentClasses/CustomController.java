package org.example.trialControlPanel.parentClasses;

import javafx.stage.Stage;

public abstract class CustomController {
    private Stage stage;
    private Core core;

    public void setCore(Core core) {
        this.core = core;
    }
    public Core getCore() {
        return core;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    public Stage getStage() {
        return stage;
    }

    // can be used to set up anything dependent on core
    public void setup() {}
}
