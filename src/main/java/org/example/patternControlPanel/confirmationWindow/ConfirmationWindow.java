package org.example.patternControlPanel.confirmationWindow;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.patternControlPanel.utils.VoidFunction;

public class ConfirmationWindow {
	
	private Stage primaryStage;
	private VoidFunction yesFunction;

	public ConfirmationWindow() {
		try {
			// load FXML file
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/Confirmation.fxml"));
			
			// create a new stage
			primaryStage = new Stage();
			
			// load parent node and create a scene
			Parent root = loader.load();
			
			ConfirmationController controller = loader.getController();
			controller.setApplication(this);
			
			// setup stage and scene
			primaryStage.setTitle("ARE YOU SURE?????");
			primaryStage.setScene(new Scene(root));
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public VoidFunction getYesFunction() {
		return yesFunction;
	}
	public void setYesFunction(VoidFunction f) {
		this.yesFunction = f;
	}
	
	public void closeWindow() {
		primaryStage.close();
	}
}
