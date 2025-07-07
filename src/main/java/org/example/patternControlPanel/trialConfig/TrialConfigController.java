package org.example.patternControlPanel.trialConfig;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.pattern.*;
import org.example.patternControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.patternControlPanel.utils.FilteredTextField;

public class TrialConfigController extends CustomController {
	@FXML
	public void initialize() {
		speedTextField.setErrorMessage("Enter a non-negative number");
		speedTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.parseDouble(input) >= 0);

		bandWidthTextField.setErrorMessage("Enter a number greater than 0");
		bandWidthTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.parseDouble(input) > 0);

		dimPercentTextField.setErrorMessage("Enter a number from 0-100 (inclusive)");
		dimPercentTextField.setValidationFunction(input -> FilteredTextField.VALID_DOUBLE.test(input)
				&& Double.parseDouble(input) >= 0 && Double.parseDouble(input) <= 100);

		trialNameTextField.setErrorMessage("Cannot be empty");
		trialNameTextField.setValidationFunction(FilteredTextField.NON_EMPTY);
		trialNameTextField.getTextField().textProperty().addListener((observer, oldText, newText) -> {
			updateSaveButtonsEnabled();
		});
	}

	// initial pattern params
	@FXML
	private Button directionCCButton, directionCCWButton, directionBothButton;
	@FXML
	private FilteredTextField speedTextField, bandWidthTextField;
	@FXML
	private Slider brightnessLightSlider, brightnessDarkSlider;
	@FXML
	private Label brightnessLightLabel, brightnessDarkLabel;

	// trial params
	@FXML
	private FilteredTextField trialNameTextField, dimPercentTextField, maxTestsTextField, testTimeTextField, restTimeTextField;


	@FXML
	private Canvas patternPreviewCanvas;

	// stores current trial data
	private TrialConfig currentTrial;
	// draws the patternControlPanel.pattern preview onto the canvas
	private PatternDrawer patternPreviewDrawer;

	@Override
	public void setup() {
		currentTrial = TrialSaver.DEFAULT_TRIAL;
		patternPreviewDrawer = new PatternDrawer(getSceneManager().getStartMenuController().getStartMenuMonitorFormat(), currentTrial.getInitialPattern(), patternPreviewCanvas, SimulatedSurface.FLAT);
		useTrial(TrialSaver.getAllTrialNames()[0]);
	}
	@FXML
	private void handleDirectionCCClick() {
		currentTrial.getInitialPattern().setDirection(PatternDirection.CLOCKWISE);
	}
	@FXML
	private void handleDirectionCCWClick() { currentTrial.getInitialPattern().setDirection(PatternDirection.COUNTER_CLOCKWISE); }
	@FXML
	private void handleDirectionBothClick() { currentTrial.getInitialPattern().setDirection(PatternDirection.BOTH); }

	@FXML
	private void handleBrightnessLightDrag() {
		currentTrial.getInitialPattern().setLightBrightness(brightnessLightSlider.getValue());
		brightnessLightLabel.setText("" + brightnessLightSlider.getValue());
	}
	@FXML
	private void handleBrightnessDarkDrag() {
		currentTrial.getInitialPattern().setDarkBrightness(brightnessDarkSlider.getValue());
		brightnessDarkLabel.setText("" + brightnessDarkSlider.getValue());
	}

	public void useTrial(String name) {
		currentTrial = TrialSaver.getTrial(name);

		trialNameTextField.getTextField().setText(currentTrial.getName());

		switch (currentTrial.getInitialPattern().getDirection()) {
			case CLOCKWISE -> directionCCButton.fire();
			case COUNTER_CLOCKWISE -> directionCCWButton.fire();
			case BOTH -> directionBothButton.fire();
		}
		speedTextField.getTextField().setText("" + currentTrial.getInitialPattern().getSpeed());
		brightnessLightSlider.setValue(currentTrial.getInitialPattern().getLightBrightness());
		brightnessDarkSlider.setValue(currentTrial.getInitialPattern().getDarkBrightness());
		bandWidthTextField.getTextField().setText("" + currentTrial.getInitialPattern().getBandWidth());
		patternPreviewDrawer.setPatternData(currentTrial.getInitialPattern());

		dimPercentTextField.getTextField().setText("" + currentTrial.getDimPercent());
		maxTestsTextField.getTextField().setText("" + currentTrial.getMaxTests());
		testTimeTextField.getTextField().setText("" + currentTrial.getTestTime());
		restTimeTextField.getTextField().setText("" + currentTrial.getRestTime());
	}

	private boolean allTextFieldsValid() {
		return speedTextField.hasValidInput()
				&& bandWidthTextField.hasValidInput()
				&& dimPercentTextField.hasValidInput();
	}

	private void updateCurrentTrialToTextFields() {
		if (speedTextField.hasValidInput())
			currentTrial.getInitialPattern().setSpeed(speedTextField.getDoubleInput());
		currentTrial.getInitialPattern().setLightBrightness(brightnessLightSlider.getValue());
		currentTrial.getInitialPattern().setDarkBrightness(brightnessDarkSlider.getValue());
		if (bandWidthTextField.hasValidInput())
			currentTrial.getInitialPattern().setBandWidth(bandWidthTextField.getDoubleInput());

		currentTrial.setName("");
		if (dimPercentTextField.hasValidInput())
			currentTrial.setDimPercent(dimPercentTextField.getDoubleInput());
		currentTrial.setMaxTests(maxTestsTextField.getIntegerInput());
		currentTrial.setTestTime(testTimeTextField.getDoubleInput());
		currentTrial.setRestTime(restTimeTextField.getDoubleInput());
		if (trialNameTextField.hasValidInput())
			currentTrial.setName(trialNameTextField.getText());
	}

//
//	// window that opens the saved patterns
//	@FXML
//	public void openSavedPatternsWindow() {
//		FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/SavedTrials.fxml"));
//
//	    Parent secondaryRoot;
//		try {
//			secondaryRoot = loader.load();
//
//			SavedTrialsController controller = loader.getController();
//			controller.setControlPanelController(this);
//
//		    Stage secondaryStage = new Stage();
//		    secondaryStage.setTitle("Pattern Variables");
//		    secondaryStage.setScene(new Scene(secondaryRoot, 300, 200));
//		    secondaryStage.show();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	@FXML
	private void handlePlayPatternPreviewClick() {
		patternPreviewDrawer.togglePlaying();
	}

	public void updateSaveButtonsEnabled() {
		if (!trialNameTextField.hasValidInput()) {
			saveAsButton.setDisable(true);
			saveButton.setDisable(true);
		}
		else if (TrialSaver.hasTrial(trialNameTextField.getText())) {
			saveAsButton.setDisable(true);
			saveButton.setDisable(false);
		}
		else {
			saveAsButton.setDisable(false);
			saveButton.setDisable(true);
		}
	}
	@FXML
	private void handleBackToStartClick() {
		getSceneManager().getPrimaryStage().setScene(getSceneManager().getStartMenuScene());
	}
	@FXML
	private void handleEditClick() {
		new SavedTrialsApplication(getSceneManager()).start(new Stage());
	}
	@FXML
	private Button saveAsButton;
	@FXML
	private Button saveButton;
	@FXML
	private void handleSaveClick() {
		updateCurrentTrialToTextFields();
		TrialSaver.addTrial(currentTrial);
		updateSaveButtonsEnabled();
	}
	
}
