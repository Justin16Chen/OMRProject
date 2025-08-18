package org.example.trialControlPanel.trialConfig;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.pattern.*;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.utils.FilteredTextField;
import org.example.trialControlPanel.utils.TimeTextField;

import java.io.IOException;

public class TrialConfigController extends CustomController {
	@FXML
	public void initialize() {
		speedTextField.setErrorMessage("Enter a non-negative number");
		speedTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.parseDouble(input) >= 0);

		bandWidthTextField.setErrorMessage("Enter a number greater than 0");
		bandWidthTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.parseDouble(input) > 0);

		dimAmountTextField.setErrorMessage("Enter an integer from 0-255 (inclusive)");
		dimAmountTextField.setValidationFunction(input -> FilteredTextField.VALID_INTEGER.test(input)
				&& Integer.parseInt(input) >= 0 && Integer.parseInt(input) <= 100);

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
	private Button directionCCButton, directionCCWButton;
	@FXML
	private FilteredTextField speedTextField, bandWidthTextField, brightnessLightTextField, brightnessDarkTextField;
	@FXML
	private Slider brightnessLightSlider, brightnessDarkSlider;

	// trial params
	@FXML
	private FilteredTextField trialNameTextField, dimAmountTextField, maxTestsTextField;
	@FXML
	private TimeTextField testTimeTextField, restTimeTextField;


	@FXML
	private Canvas patternPreviewCanvas;

	// stores current trial data
	private TrialConfig currentTrial;
	// draws the patternControlPanel.pattern preview onto the canvas
	private PatternDrawer patternPreviewDrawer;

	@Override
	public void setup() {
		TrialConfig initialTrial = TrialSaver.NEW_DEFAULT_TRIAL;
		patternPreviewDrawer = new PatternDrawer(getCore().getStartMenuController().getStartMenuMonitorFormat(), initialTrial.getInitialPattern(), patternPreviewCanvas, SimulatedSurface.FLAT);
		useTrial(TrialSaver.NEW_DEFAULT_TRIAL);
	}
	@FXML
	private void handleDirectionCCClick() {
		currentTrial.getInitialPattern().setDirection(PatternDirection.CLOCKWISE);
	}
	@FXML
	private void handleDirectionCCWClick() { currentTrial.getInitialPattern().setDirection(PatternDirection.COUNTER_CLOCKWISE); }

	private void handleBrightnessLightDrag() {
		currentTrial.getInitialPattern().setLightBrightness((int) brightnessLightSlider.getValue());
		brightnessLightTextField.getTextField().setText("" + Math.round(brightnessLightSlider.getValue()));
	}
	private void handleBrightnessDarkDrag() {
		currentTrial.getInitialPattern().setDarkBrightness((int) brightnessDarkSlider.getValue());
		brightnessDarkTextField.getTextField().setText("" + Math.round(brightnessDarkSlider.getValue()));
	}

	public void useTrial(TrialConfig trial) {
		currentTrial = trial;

		trialNameTextField.getTextField().setText(currentTrial.getName());

		switch (currentTrial.getInitialPattern().getDirection()) {
			case CLOCKWISE -> directionCCButton.fire();
			case COUNTER_CLOCKWISE -> directionCCWButton.fire();
		}
		speedTextField.getTextField().setText("" + currentTrial.getInitialPattern().getSpeed());
		brightnessLightSlider.setValue(currentTrial.getInitialPattern().getLightBrightness());
		brightnessLightTextField.getTextField().setText("" + currentTrial.getInitialPattern().getLightBrightness());
		brightnessDarkSlider.setValue(currentTrial.getInitialPattern().getDarkBrightness());
		brightnessDarkTextField.getTextField().setText("" + currentTrial.getInitialPattern().getDarkBrightness());
		bandWidthTextField.getTextField().setText("" + currentTrial.getInitialPattern().getBandWidth());

		dimAmountTextField.getTextField().setText("" + currentTrial.getDimAmount());
		maxTestsTextField.getTextField().setText("" + currentTrial.getMaxTests());
		testTimeTextField.getTextField().setText(TimeTextField.secondsToMMSS(currentTrial.getTestTime()));
		restTimeTextField.getTextField().setText(TimeTextField.secondsToMMSS(currentTrial.getRestTime()));

		patternPreviewDrawer.setPatternData(currentTrial.getInitialPattern());
	}

	private boolean allTextFieldsValid() {
		return speedTextField.hasValidInput()
				&& bandWidthTextField.hasValidInput()
				&& brightnessLightTextField.hasValidInput()
				&& brightnessDarkTextField.hasValidInput()
				&& dimAmountTextField.hasValidInput();
	}

	private void updateCurrentTrialToTextFields() {
		if (speedTextField.hasValidInput())
			currentTrial.getInitialPattern().setSpeed(speedTextField.getDoubleInput());
		currentTrial.getInitialPattern().setLightBrightness((int) brightnessLightSlider.getValue());
		currentTrial.getInitialPattern().setDarkBrightness((int) brightnessDarkSlider.getValue());
		if (bandWidthTextField.hasValidInput())
			currentTrial.getInitialPattern().setBandWidth(bandWidthTextField.getDoubleInput());

		currentTrial.setName("");
		if (dimAmountTextField.hasValidInput())
			currentTrial.setDimAmount(dimAmountTextField.getIntegerInput());
		currentTrial.setMaxTests(maxTestsTextField.getIntegerInput());
		currentTrial.setTestTime(testTimeTextField.getSeconds());
		currentTrial.setRestTime(restTimeTextField.getSeconds());
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
		try {
			getCore().loadStartMenu();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		getCore().getPrimaryStage().setScene(getCore().getStartMenuScene());
		getCore().getStartMenuController().setup();
    }
	@FXML
	public void handleEditClick() {
		new SavedTrialsApplication(getCore()).start(new Stage());
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
