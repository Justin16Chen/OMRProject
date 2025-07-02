package org.example.patternControlPanel.pattern;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.controlPanel.TrialConfigController;

public class PatternVariablesController {

	@FXML
	private Button selectPatternButton;
	@FXML
	private void activateSelectedPattern() {
		String selectedName = patternSelectDropdown.getValue();
		// use default patternControlPanel.pattern if user selects empty dropdown option
		controlPanelController.usePattern(selectedName == null ? PatternVariableManager.DEFAULT_PATTERN_DATA.getName() : selectedName);
	}
		
	@FXML
	private Button deletePatternButton;
	@FXML
	private void deleteSelectedPattern() {
		PatternVariableManager.removePattern(patternSelectDropdown.getValue());
		updateDropdown();
	}
	
	@FXML
	private ComboBox<String> patternSelectDropdown;
	
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
		patternSelectDropdown.getItems().clear();
		for (Pattern pattern : PatternVariableManager.getAllPatternsFromFile())
			patternSelectDropdown.getItems().add(pattern.getName());
	}
}
