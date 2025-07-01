package org.example.patternControlPanel.pattern;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.example.patternControlPanel.utils.FilteredTextField;

public class SavePatternController {
	private Pattern pattern;

	@FXML
	private FilteredTextField nameTextField;
	@FXML
	private Button saveButton;
	@FXML
	private void savePattern() {
		if (!nameTextField.hasValidInput())
			return;
		pattern.setName(nameTextField.getText());
		PatternVariableManager.addPattern(pattern);
	}
	
	@FXML
	private void initialize() {
		nameTextField.setValidationFunction((input) -> FilteredTextField.NON_EMPTY.test(input) && !PatternVariableManager.hasPattern(input));
		nameTextField.setErrorMessage("Name must be non-empty and unique");
	}
	
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}
	
}
