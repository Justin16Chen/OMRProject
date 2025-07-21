package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.trialControlPanel.sceneManager.CustomApplication;
import org.example.trialControlPanel.sceneManager.Core;

public class QueueTrialApplication extends CustomApplication {

    public QueueTrialApplication(Core core) {
        super(core);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/QueueTrial.fxml"));

            stage.setTitle("Trial Queue");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            QueueTrialController controller = loader.getController();
            controller.setCore(getCore());
            controller.setStage(stage);
            controller.updateSavedTrialsComboBox();

        } catch(Exception e) {
            System.out.println("QUEUE TRIAL APP FAILED TO LAUNCH");
        }
    }
}
