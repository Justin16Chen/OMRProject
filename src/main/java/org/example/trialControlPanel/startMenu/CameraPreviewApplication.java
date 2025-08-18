package org.example.trialControlPanel.startMenu;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.parentClasses.CustomApplication;

public class CameraPreviewApplication extends CustomApplication {
    public CameraPreviewApplication(Core core) {
        super(core);
    }

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/CameraPreview.fxml"));

            stage.setTitle("Camera Preview");
            stage.setScene(new Scene(loader.load()));
            stage.show();

            CameraPreviewController controller = loader.getController();
            controller.setCore(getCore());
            controller.setStage(stage);
            controller.setup();

        } catch(Exception e) {
            System.out.println("CAMERA PREVIEW APP FAILED TO LAUNCH");
        }
    }
}
