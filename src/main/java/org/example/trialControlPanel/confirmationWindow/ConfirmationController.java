package org.example.trialControlPanel.confirmationWindow;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class ConfirmationController {
	
	private ConfirmationWindow application;
	@FXML
	protected Button yesButton;
	@FXML 
	protected Button noButton;
	
	public void setApplication(ConfirmationWindow application) {
		this.application = application;
	}
	
	@FXML
	private void onYesButtonClick() {
		if (application.getYesFunction() != null)
			application.getYesFunction().execute();
		application.closeWindow();
	}
	
	@FXML 
	private void onNoButtonClick() {
		application.closeWindow();
	}
}
