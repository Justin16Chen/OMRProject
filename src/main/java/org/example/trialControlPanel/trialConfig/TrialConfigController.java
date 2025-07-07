package org.example.trialControlPanel.trialConfig;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.pattern.*;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.utils.FilteredTextField;

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

		brightnessDarkSlider.valueProperty().addListener((obs, oldVal, newVal) -> handleBrightnessDarkDrag());
		brightnessLightSlider.valueProperty().addListener((obs, oldVal, newVal) -> handleBrightnessLightDrag());
		brightnessDarkTextField.setErrorMessage("!in 0-255");
		brightnessLightTextField.setErrorMessage("!in 0-255");
		brightnessDarkTextField.setValidationFunction(str -> FilteredTextField.VALID_DOUBLE.test(str)
				&& Double.parseDouble(str) >= 0 && Double.parseDouble(str) <= 255);
		brightnessLightTextField.setValidationFunction(str -> FilteredTextField.VALID_DOUBLE.test(str)
				&& Double.parseDouble(str) >= 0 && Double.parseDouble(str) <= 255);
		brightnessDarkTextField.getTextField().textProperty().addListener((obs, oldText, newText) -> {
			if (brightnessDarkTextField.hasValidInput())
				brightnessDarkSlider.setValue(Double.parseDouble(newText));
		});
		brightnessLightTextField.getTextField().textProperty().addListener((obs, oldText, newText) -> {
			if (brightnessLightTextField.hasValidInput())
				brightnessLightSlider.setValue(Double.parseDouble(newText));
		});
	}

	// initial pattern params
	@FXML
	private Button directionCCButton, directionCCWButton, directionBothButton;
	@FXML
	private FilteredTextField speedTextField, bandWidthTextField, brightnessLightTextField, brightnessDarkTextField;
	@FXML
	private Slider brightnessLightSlider, brightnessDarkSlider;

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
		String initialTrialName = TrialSaver.getAllTrialNames()[0];
		TrialConfig initialTrial = TrialSaver.getTrial(initialTrialName);
		patternPreviewDrawer = new PatternDrawer(getSceneManager().getStartMenuController().getStartMenuMonitorFormat(), initialTrial.getInitialPattern(), patternPreviewCanvas, SimulatedSurface.FLAT);
		useTrial(initialTrialName);
	}
	@FXML
	private void handleDirectionCCClick() {
		currentTrial.getInitialPattern().setDirection(PatternDirection.CLOCKWISE);
	}
	@FXML
	private void handleDirectionCCWClick() { currentTrial.getInitialPattern().setDirection(PatternDirection.COUNTER_CLOCKWISE); }
	@FXML
	private void handleDirectionBothClick() { currentTrial.getInitialPattern().setDirection(PatternDirection.BOTH); }

	private void handleBrightnessLightDrag() {
		currentTrial.getInitialPattern().setLightBrightness((int) brightnessLightSlider.getValue());
		brightnessLightTextField.getTextField().setText("" + Math.round(brightnessLightSlider.getValue()));
	}
	private void handleBrightnessDarkDrag() {
		currentTrial.getInitialPattern().setDarkBrightness((int) brightnessDarkSlider.getValue());
		brightnessDarkTextField.getTextField().setText("" + Math.round(brightnessDarkSlider.getValue()));
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
		brightnessLightTextField.getTextField().setText("" + currentTrial.getInitialPattern().getLightBrightness());
		brightnessDarkSlider.setValue(currentTrial.getInitialPattern().getDarkBrightness());
		brightnessDarkTextField.getTextField().setText("" + currentTrial.getInitialPattern().getDarkBrightness());
		bandWidthTextField.getTextField().setText("" + currentTrial.getInitialPattern().getBandWidth());

		dimPercentTextField.getTextField().setText("" + currentTrial.getDimPercent());
		maxTestsTextField.getTextField().setText("" + currentTrial.getMaxTests());
		testTimeTextField.getTextField().setText("" + currentTrial.getTestTime());
		restTimeTextField.getTextField().setText("" + currentTrial.getRestTime());

		patternPreviewDrawer.setPatternData(currentTrial.getInitialPattern());
	}

	private boolean allTextFieldsValid() {
		return speedTextField.hasValidInput()
				&& bandWidthTextField.hasValidInput()
				&& brightnessLightTextField.hasValidInput()
				&& brightnessDarkTextField.hasValidInput()
				&& dimPercentTextField.hasValidInput();
	}

	private void updateCurrentTrialToTextFields() {
		if (speedTextField.hasValidInput())
			currentTrial.getInitialPattern().setSpeed(speedTextField.getDoubleInput());
		currentTrial.getInitialPattern().setLightBrightness((int) brightnessLightSlider.getValue());
		currentTrial.getInitialPattern().setDarkBrightness((int) brightnessDarkSlider.getValue());
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
		if (!allTextFieldsValid())
			return;

		TrialSaver.addTrial(currentTrial);
		updateSaveButtonsEnabled();
	}
	
}
