package org.example.patternControlPanel.pattern;

import javafx.fxml.FXML;
import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.example.patternControlPanel.utils.FilteredTextField;

public class SaveTrialController {
	private TrialConfig trial;

	@FXML
	private FilteredTextField nameTextField;
	@FXML
	private void saveTrial() {
		if (!nameTextField.hasValidInput())
			return;
		trial.setName(nameTextField.getText());
		TrialSaver.addTrial(trial);
	}
	
	@FXML
	private void initialize() {
		nameTextField.setCheckInputType(FilteredTextField.CheckInputType.ON_COMMAND);
		nameTextField.setValidationFunction((input) -> FilteredTextField.NON_EMPTY.test(input) && !TrialSaver.hasTrial(input));
		nameTextField.setErrorMessage("Name must be non-empty and unique");
	}
	
	public void setTrial(TrialConfig trial) {
		this.trial = trial;
	}
	
}
