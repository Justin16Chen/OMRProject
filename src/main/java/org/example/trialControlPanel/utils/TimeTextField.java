package org.example.trialControlPanel.utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeTextField extends FilteredTextField {
    public static String secondsToMMSS(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    public TimeTextField() {
        super();
        getTextField().setText("00:00");
        getTextField().setPromptText("mm:ss");
        setValidationFunction(FilteredTextField.VALID_MM_SS_TIME);
        setErrorMessage("Not in mm:ss format");
        setPrefWidth(48);
    }
    public int getSeconds() {
        if (hasValidInput()) {
            LocalTime localTime = LocalTime.parse("00:" + getText(), DateTimeFormatter.ofPattern("HH:mm:ss"));
            return localTime.toSecondOfDay();
        }
        return -1;
    }
}
