package org.example.patternControlPanel.trialConfig;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.pattern.*;
import org.example.patternControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.patternControlPanel.utils.FilteredTextField;

import java.util.Arrays;

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
	}

	// initial pattern params
	@FXML
	private Button directionCCButton, directionCCWButton, directionBothButton;
	@FXML
	private FilteredTextField speedTextField, bandWidthTextField;
	@FXML
	private Slider brightnessLightSlider, brightnessDarkSlider;

	// trial params
	@FXML
	private Label trialNameLabel;
	@FXML
	private FilteredTextField dimPercentTextField, maxTestsTextField, testTimeTextField, restTimeTextField;


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

	}
	@FXML
	private void handleBrightnessDarkDrag() {

	}

	public void useTrial(String name) {
		currentTrial = TrialSaver.getTrial(name);

		trialNameLabel.setText("Current Trial: " + currentTrial.getName());

		switch (currentTrial.getInitialPattern().getDirection()) {
			case CLOCKWISE -> directionCCButton.fire();
			case COUNTER_CLOCKWISE -> directionCCWButton.fire();
			case BOTH -> directionBothButton.fire();
		}
		speedTextField.getTextField().setText("" + currentTrial.getInitialPattern().getSpeed());
		brightnessLightSlider.setValue(currentTrial.getInitialPattern().getLightBrightness());
		brightnessDarkSlider.setValue(0);
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
	}

	// window that allows user to name and save new patternControlPanel.pattern
//	@FXML
//	private void openPatternSaveWindow() {
//		FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/SavePattern.fxml"));
//
//	    Parent secondaryRoot;
//		try {
//			secondaryRoot = loader.load();
//
//			SaveTrialController controller = loader.getController();
//			controller.setPattern(currentPattern);
//
//		    Stage secondaryStage = new Stage();
//		    secondaryStage.setTitle("Save New Pattern");
//		    secondaryStage.setScene(new Scene(secondaryRoot, 300, 200));
//		    secondaryStage.show();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//
//	// window that opens the saved patterns
//	@FXML
//	public void openSavedPatternsWindow() {
//		FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/PatternVariables.fxml"));
//
//	    Parent secondaryRoot;
//		try {
//			secondaryRoot = loader.load();
//
//			TrialVariablesController controller = loader.getController();
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

	@FXML
	private void handleBackToStartClick() {
		getSceneManager().getPrimaryStage().setScene(getSceneManager().getStartMenuScene());
	}
	@FXML
	private void handleViewTrialsClick() {
		System.out.println(Arrays.toString(TrialSaver.getAllTrialNames()));
	}
	@FXML
	private void handleSaveTrialClick() {
		updateCurrentTrialToTextFields();
		TrialSaver.addTrial(currentTrial);
	}
	@FXML
	private void handleEditTrialClick() {

	}
	
}
