package org.example.patternControlPanel.trial;

import org.example.patternControlPanel.pattern.Pattern;


// meant to store ONLY the configuration data of each trial - not the data gathered from actual experimentation
public class TrialConfig {
    private Pattern startPattern;

    private double trialTime; // seconds
    private double restTime; // seconds

    private double dimPercent; // ex: 15%, 5%
    private double brightnessAccuracyThreshold; // (percentage) - how accurate dimness must be before stopping trials
    private int maxTrials;

    public TrialConfig() {

    }
}
