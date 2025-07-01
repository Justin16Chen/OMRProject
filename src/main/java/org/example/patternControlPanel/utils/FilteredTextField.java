package org.example.patternControlPanel.utils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.function.Predicate;

public class FilteredTextField extends VBox {
	
	// pre-defined validation functions
	/**
	 * returns predicate that returns if a string is not null and has a length greater than 0
	 */
	public static Predicate<String> NON_EMPTY = str -> {
		return str != null && !str.isEmpty();
	};
	/**
	 * returns predicate that returns if a string can be parsed to an integer
	 */
	public static Predicate<String> VALID_INTEGER = str -> {
		if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
	};
	/**
	 * returns predicate that returns if a string can be parsed to a double
	 */
	public static Predicate<String> VALID_DOUBLE = str -> {
		if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
	};

	/**
	 * returns true if str is not null and has a length greater than 0
	 */
	public static boolean isNonEmpty(String str) {
		return str != null && !str.isEmpty();
	}

	/**
	 * returns true if str can be parsed to an integer
	 */
	public static boolean isInteger(String str) {
		if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
	}

	/**
	 * returns true if str can be parsed to a double
	 */
	public static boolean isDouble(String str) {
		if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
	}
	
	public enum CheckInputType {
		ALWAYS,
		ON_COMMAND
	}

    @FXML private Label errorLabel;
    @FXML private TextField textField;
    private CheckInputType checkInputType;

    private String errorMessage;
    private Predicate<String> inputValidation;

    public FilteredTextField() {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/FilteredTextField.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        
        inputValidation = NON_EMPTY;
        checkInputType = CheckInputType.ALWAYS;

        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
        	if (checkInputType == CheckInputType.ALWAYS)
        		hasValidInput();
        });
    }
    
    /**
     * sets when to check for a valid input and show/hide the error message
     */
    public void setCheckInputType(CheckInputType checkType) {
    	checkInputType = checkType;
    }

    public Label getErrorLabel() {
        return errorLabel;
    }

    public TextField getTextField() {
        return textField;
    }

    public String getText() {
        return textField.getText();
    }

	/**
	 * sets the error message to be displayed whenever the text box has invalid text
	 */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

	/**
	 * sets the validation function for this text field
	 * whenever the user inputs text that makes this function return false, the error message appears
	 */
    public void setValidationFunction(Predicate<String> inputValidation) {
        this.inputValidation = inputValidation;
    }
    
    /**
     * checks the text field and returns whether or not the current text is valid
	 * also updates the error message
     */
    public boolean hasValidInput() {
    	if (inputValidation == null)
    		throw new RuntimeException("valid input function not defined");
    	boolean valid = inputValidation.test(textField.getText());
    	if (valid) 
    		errorLabel.setText("");
    	else 
    		errorLabel.setText(errorMessage);
    	return valid;
    }

	/**
	 * casts the text to an integer
	 * throws an error if the text cannot be casted
	 */
    public int getIntegerInput() {
    	return Integer.valueOf(textField.getText());
    }
	/**
	 * casts the text to a double
	 * throws an error if the text cannot be casted
	 */
    public double getDoubleInput() {
    	return Double.valueOf(textField.getText());
    }
	/**
	 * returns whatever is in the text field
	 */
    public String getTextInput() {
    	return textField.getText();
    }
}
