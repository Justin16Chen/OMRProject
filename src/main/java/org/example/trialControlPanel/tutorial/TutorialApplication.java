package org.example.trialControlPanel.tutorial;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.parentClasses.CustomApplication;

public class TutorialApplication extends CustomApplication {
    public TutorialApplication(Core core) {
        super(core);
    }

    @Override
    public void start(Stage stage) throws Exception {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/Tutorial.fxml"));

            stage.setTitle("Tutorial");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            TutorialController controller = loader.getController();
            controller.setCore(getCore());
            controller.setStage(stage);
            controller.setup();

        } catch(Exception e) {
            System.out.println("TUTORIAL APP FAILED TO LAUNCH");
        }
    }
}
