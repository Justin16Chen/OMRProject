package org.example.patternControlPanel.trialConfig;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import org.example.patternControlPanel.sceneManager.CustomController;

public class SavedTrialsController extends CustomController {

	@FXML
	private Button selectButton, deleteButton, clearAllButton;
	@FXML
	private void handleSelectClick() {
		String selectedName = trialSelectDropdown.getValue();
		if (selectedName != null) {
			getSceneManager().getTrialConfigController().useTrial(selectedName);
			getStage().close();
			getSceneManager().getTrialConfigController().updateSaveButtonsEnabled();
		}
		else
			throw new IllegalStateException("selected trial name cannot be null"); // should never run
	}
		
	@FXML
	private void handleDeleteClick() {
		if (trialSelectDropdown.getValue() == null)
			throw new IllegalStateException("selected trial name cannot be null"); // should never run

		TrialSaver.removeTrial(trialSelectDropdown.getValue());
		updateDropdown();

		if (trialSelectDropdown.getItems().isEmpty())
			clearAllButton.setDisable(true);

		getSceneManager().getTrialConfigController().updateSaveButtonsEnabled();
	}
	@FXML
	private void handleClearAllClick() {
		TrialSaver.clearAllTrials();
		updateDropdown();
		clearAllButton.setDisable(true);
		getSceneManager().getTrialConfigController().updateSaveButtonsEnabled();
	}
	
	@FXML
	private ComboBox<String> trialSelectDropdown;
	@FXML
	private void handleSelectDropdownChanged() {
		if (trialSelectDropdown.getValue() == null) {
			selectButton.setDisable(true);
			deleteButton.setDisable(true);
		}
		else {
			selectButton.setDisable(false);
			deleteButton.setDisable(false);
		}
	}

	@FXML
	public void initialize() {
		updateDropdown();
		if (trialSelectDropdown.getItems().isEmpty()) {
			clearAllButton.setDisable(true);
			selectButton.setDisable(true);
			deleteButton.setDisable(true);
		}
		else {
			clearAllButton.setDisable(false);
			selectButton.setDisable(false);
			deleteButton.setDisable(false);
		}
	}
	
	// updates the contents of the dropdown menu to match saved trials
	private void updateDropdown() {
		trialSelectDropdown.getItems().clear();
		for (String trialName : TrialSaver.getAllTrialNames())
			trialSelectDropdown.getItems().add(trialName);
		if (!trialSelectDropdown.getItems().isEmpty())
			trialSelectDropdown.setValue(trialSelectDropdown.getItems().getFirst());
	}
}
