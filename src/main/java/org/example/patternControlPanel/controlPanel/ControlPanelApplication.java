package org.example.patternControlPanel.controlPanel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;


public class ControlPanelApplication extends Application {

	@Override
	public void start(Stage primaryStage) {
		try {
			// load FXML file
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/ControlPanel.fxml"));
			
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
	
	public static void main(String[] args) {
		 // Get the local graphics environment
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        // Get all screen devices (monitors)
        GraphicsDevice[] screens = ge.getScreenDevices();

        // Loop through each screen and print its resolution
        for (int i = 0; i < screens.length; i++) {
            GraphicsDevice screen = screens[i];
            DisplayMode displayMode = screen.getDisplayMode();
            int width = displayMode.getWidth();
            int height = displayMode.getHeight();
        }
		launch(args);
	}
}
