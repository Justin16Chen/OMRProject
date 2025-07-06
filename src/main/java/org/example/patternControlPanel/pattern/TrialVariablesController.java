package org.example.patternControlPanel.pattern;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.trialConfig.TrialConfigController;

public class TrialVariablesController {

	@FXML
	private void activateSelectedPattern() {
		String selectedName = trialSelectDropdown.getValue();
		// use default patternControlPanel.pattern if user selects empty dropdown option
		controlPanelController.useTrial(selectedName == null ? TrialSaver.DEFAULT_TRIAL.getName() : selectedName);
	}
		
	@FXML
	private void deleteSelectedPattern() {
		TrialSaver.removeTrial(trialSelectDropdown.getValue());
		updateDropdown();
	}
	
	@FXML
	private ComboBox<String> trialSelectDropdown;
	
	private TrialConfigController controlPanelController;
	public void setControlPanelController(TrialConfigController controller) {
		this.controlPanelController = controller;
	}
	
	@FXML
	public void initialize() {
		updateDropdown();
	}
	
	// updates the contents of the dropdown menu to match the JSON file
	private void updateDropdown() {
		trialSelectDropdown.getItems().clear();
		for (String trialName : TrialSaver.getAllTrialNames())
			trialSelectDropdown.getItems().add(trialName);
	}
}
