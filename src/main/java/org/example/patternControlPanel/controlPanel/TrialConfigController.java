package org.example.patternControlPanel.controlPanel;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.patternControlPanel.confirmationWindow.ConfirmationWindow;
import org.example.patternControlPanel.display.DisplayController;
import org.example.patternControlPanel.display.TimerWindow;
import org.example.patternControlPanel.pattern.*;
import org.example.patternControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.patternControlPanel.utils.FilteredTextField;

import java.io.IOException;

public class TrialConfigController {

	@FXML
	private FilteredTextField controlPanelMonitorTextField;
	@FXML
	private FilteredTextField targetMonitorTextField;
	@FXML
	private Button directionClockwiseButton;
	@FXML
	private Button directionCounterClockwiseButton;
	@FXML
	private FilteredTextField speedTextField;
	@FXML
	private FilteredTextField brightnessTextField;
	@FXML
	private FilteredTextField bandWidthTextField;
	@FXML
	private Label patternNameLabel;
	@FXML
	private Button saveNewPatternButton;
	@FXML
	private Button savePatternButton;
	@FXML
	private Button useSavedPatternButton;
	@FXML
	private Button runCurrentPatternButton;
	ConfirmationWindow confirmationWindow;  	// confirmation before running patternControlPanel.pattern
	
	// stores current patternControlPanel.pattern data
	private Pattern currentPattern;
	
	@FXML
	private Button playPatternPreviewButton;
	@FXML
	private Canvas patternPreviewCanvas;
	private PatternDrawer patternPreviewDrawer; // draws the patternControlPanel.pattern preview onto the canvas
	private MonitorFormat controlPanelMonitorFormat, targetMonitorFormat;
	
	private Stage displayStage;
	
	@FXML
	public void initialize() {

		currentPattern = new Pattern("default", PatternDirection.CLOCKWISE, 0, 0, 0);
		
		// assumes the number of screens doesn't change, but that is ok for me
		controlPanelMonitorTextField.setErrorMessage("Enter an integer from 1-" + MonitorFormat.getNumScreens() + " inclusive");
		controlPanelMonitorTextField.setValidationFunction((input) -> FilteredTextField.isInteger(input)
				&& Integer.valueOf(input) > 0 && MonitorFormat.hasScreen(Integer.valueOf(input)));
		controlPanelMonitorTextField.getTextField().setText("1");
		targetMonitorTextField.setErrorMessage("enter a positive integer");
		targetMonitorTextField.setValidationFunction(FilteredTextField.VALID_INTEGER);
		targetMonitorTextField.getTextField().setText("1");
		 
		speedTextField.setErrorMessage("Enter a non-negative number");
		speedTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.valueOf(input) >= 0);
		
		brightnessTextField.setErrorMessage("Enter a number from 0-1 inclusive");
		brightnessTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.valueOf(input) >= 0 && Double.valueOf(input) <= 1);

		bandWidthTextField.setErrorMessage("Enter a number greater than 0");
		bandWidthTextField.setValidationFunction(input -> FilteredTextField.isDouble(input)
				&& Double.valueOf(input) > 0);

		controlPanelMonitorFormat = new MonitorFormat(1);
		targetMonitorFormat = new MonitorFormat(1);
		
		patternPreviewDrawer = new PatternDrawer(controlPanelMonitorFormat, currentPattern, patternPreviewCanvas, SimulatedSurface.FLAT);
		usePattern(PatternVariableManager.getFirstPatternName());
	}
	
	@FXML
	private void updateControlPanelMonitorFormat() {
		if (controlPanelMonitorTextField.hasValidInput()) {
			int monitorNum = Integer.valueOf(controlPanelMonitorTextField.getText());
			controlPanelMonitorFormat = new MonitorFormat(monitorNum);
		}
	}
	@FXML
	private void updateTargetMonitorFormat() {
		if (targetMonitorTextField.hasValidInput()) {
			int monitorNum = Integer.valueOf(targetMonitorTextField.getText());
			targetMonitorFormat = new MonitorFormat(monitorNum);
		}
	}

	@FXML
	private void setDirectionClockwise() {
		currentPattern.setDirection(PatternDirection.CLOCKWISE);
	}
	@FXML
	private void setDirectionCounterClockwise() {
		currentPattern.setDirection(PatternDirection.COUNTER_CLOCKWISE);
	}
	
	
	// sets all the text fields to the parameters of the patternControlPanel.pattern of the same name
	// also sets the current patternControlPanel.pattern to the patternControlPanel.pattern w the same name
	// can ignore direction???
	public void usePattern(String name) {
		currentPattern = PatternVariableManager.getPatternDataFromFile(name);
		
		speedTextField.getTextField().setText("" + currentPattern.getSpeed());
		brightnessTextField.getTextField().setText("" + currentPattern.getBrightness());
		bandWidthTextField.getTextField().setText("" + currentPattern.getBandWidth());
		
		patternNameLabel.setText("Current Pattern: " + currentPattern.getName());
		patternPreviewDrawer.setPatternData(currentPattern);
	}
	private boolean areTextFieldsValid() {
		return speedTextField.hasValidInput()
				&& brightnessTextField.hasValidInput() 
				&& bandWidthTextField.hasValidInput();
	}
	private void updateCurrentPatternToTextFields() {
		if (speedTextField.hasValidInput())
			currentPattern.setSpeed(speedTextField.getDoubleInput());
		if (brightnessTextField.hasValidInput())
			currentPattern.setBrightness(brightnessTextField.getDoubleInput());
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
	private void runPatternDisplay() {
		confirmationWindow = new ConfirmationWindow();
		confirmationWindow.setYesFunction(() -> {
			// open timer window
			TimerWindow timerWindow = new TimerWindow(controlPanelMonitorFormat);
			timerWindow.setCloseFunction(() -> displayStage.close());
			timerWindow.startTimer();
			
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/Display.fxml"));
		    Parent secondaryRoot;
			try {
				secondaryRoot = loader.load();
				
				DisplayController controller = loader.getController();
				controller.setMonitorFormat(targetMonitorFormat);
				controller.setPattern(currentPattern);
				controller.start();
			    			
			    displayStage = new Stage();
			    displayStage.setTitle("PatternDisplay");
			    displayStage.initStyle(StageStyle.UNDECORATED);
			    displayStage.setScene(new Scene(secondaryRoot, 300, 200));
			    Rectangle2D bounds = targetMonitorFormat.getBounds();
			    displayStage.setX(bounds.getMinX());
			    displayStage.setY(bounds.getMinY());
			    displayStage.setWidth(bounds.getWidth());
			    displayStage.setHeight(bounds.getHeight());
			    controller.resizeCanvas((int) bounds.getWidth(), (int) bounds.getHeight());
			    displayStage.show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	@FXML
	private void togglePatternPreviewPlaying() {
		patternPreviewDrawer.togglePlaying();
	}
	
}
