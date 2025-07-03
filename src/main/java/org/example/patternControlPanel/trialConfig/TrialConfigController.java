package org.example.patternControlPanel.trialConfig;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Slider;
import javafx.stage.Stage;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.pattern.*;
import org.example.patternControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.patternControlPanel.utils.FilteredTextField;

import java.io.IOException;

public class TrialConfigController extends CustomController {
	// initial pattern params
	@FXML
	private FilteredTextField speedTextField, bandWidthTextField;
	@FXML
	private Slider brightnessLightSlider, brightnessDarkSlider;

	// trial params
	@FXML
	private FilteredTextField dimPercentTextField, maxTestsTextField, testTimeTextField, restTimeTextField;

	// stores current patternControlPanel.pattern data
	private Pattern currentPattern;
	@FXML
	private Canvas patternPreviewCanvas;
	private PatternDrawer patternPreviewDrawer; // draws the patternControlPanel.pattern preview onto the canvas

	@FXML
	public void initialize() {
		currentPattern = new Pattern("default", PatternDirection.CLOCKWISE, 0, 0, 0, 0);
		 
		speedTextField.setErrorMessage("Enter a non-negative number");
		speedTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.parseDouble(input) >= 0);

		bandWidthTextField.setErrorMessage("Enter a number greater than 0");
		bandWidthTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.parseDouble(input) > 0);
	}

	@Override
	public void setup() {
		patternPreviewDrawer = new PatternDrawer(getSceneManager().getStartMenuController().getStartMenuMonitorFormat(), currentPattern, patternPreviewCanvas, SimulatedSurface.FLAT);
		usePattern(PatternVariableManager.getFirstPatternName());
	}

	@FXML
	private void handleDirectionCCClick() {
		currentPattern.setDirection(PatternDirection.CLOCKWISE);
	}
	@FXML
	private void handleDirectionCCWClick() { currentPattern.setDirection(PatternDirection.COUNTER_CLOCKWISE); }
	@FXML
	private void handleDirectionBothClick() { currentPattern.setDirection(PatternDirection.BOTH); }

	@FXML
	private void handleBrightnessLightDrag() {

	}
	@FXML
	private void handleBrightnessDarkDrag() {

	}

	public void usePattern(String name) {
		currentPattern = PatternVariableManager.getPatternDataFromFile(name);
		
		speedTextField.getTextField().setText("" + currentPattern.getSpeed());
		brightnessLightSlider.setValue(currentPattern.getLightBrightness());
		brightnessDarkSlider.setValue(0);
		bandWidthTextField.getTextField().setText("" + currentPattern.getBandWidth());
		patternPreviewDrawer.setPatternData(currentPattern);
	}
	private boolean areTextFieldsValid() {
		return speedTextField.hasValidInput()
				&& bandWidthTextField.hasValidInput();
	}
	private void updateCurrentPatternToTextFields() {
		if (speedTextField.hasValidInput())
			currentPattern.setSpeed(speedTextField.getDoubleInput());
		currentPattern.setLightBrightness(brightnessLightSlider.getValue());
		currentPattern.setDarkBrightness(brightnessDarkSlider.getValue());
		if (bandWidthTextField.hasValidInput())
			currentPattern.setBandWidth(bandWidthTextField.getDoubleInput());
	}
	
	// only saves patternControlPanel.pattern if all text fields have valid input
	@FXML
	private void saveNewPattern() {
		updateCurrentPatternToTextFields();
		
		// only save if all valid
		if (areTextFieldsValid()) {
			PatternVariableManager.addPattern(currentPattern);
		}
	}
	
	// saves the new params to the current patternControlPanel.pattern
	@FXML
	private void savePattern() {
		updateCurrentPatternToTextFields();
		
		// only save if all valid
		if (areTextFieldsValid() && PatternVariableManager.hasPattern(currentPattern.getName()))
				PatternVariableManager.updateSavedPattern(currentPattern);
	}

	// window that allows user to name and save new patternControlPanel.pattern
	@FXML
	private void openPatternSaveWindow() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/SavePattern.fxml"));
		
	    Parent secondaryRoot;
		try {
			secondaryRoot = loader.load();
			
			SavePatternController controller = loader.getController();
			controller.setPattern(currentPattern);
			
		    Stage secondaryStage = new Stage();
		    secondaryStage.setTitle("Save New Pattern");
		    secondaryStage.setScene(new Scene(secondaryRoot, 300, 200));
		    secondaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// window that opens the saved patterns
	@FXML
	public void openSavedPatternsWindow() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/PatternVariables.fxml"));
		
	    Parent secondaryRoot;
		try {
			secondaryRoot = loader.load();
			
			PatternVariablesController controller = loader.getController();
			controller.setControlPanelController(this);
		    			
		    Stage secondaryStage = new Stage();
		    secondaryStage.setTitle("Pattern Variables");
		    secondaryStage.setScene(new Scene(secondaryRoot, 300, 200));
		    secondaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	private void togglePatternPreviewPlaying() {
		patternPreviewDrawer.togglePlaying();
	}
	
}
