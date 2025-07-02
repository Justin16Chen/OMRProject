package org.example.patternControlPanel.controlPanel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class TrialConfigApplication extends Application {

	@Override
	public void start(Stage primaryStage) {
		try {
			// load FXML file
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/TrialConfig.fxml"));
			
			// load parent node and create a scene
			Parent root = loader.load();
			
			// setup stage and scene
			primaryStage.setTitle("Control Panel");
			primaryStage.setScene(new Scene(root));
			primaryStage.show();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
