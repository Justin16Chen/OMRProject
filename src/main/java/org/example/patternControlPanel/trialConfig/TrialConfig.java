package org.example.patternControlPanel.trialConfig;

import org.example.patternControlPanel.pattern.Pattern;

// meant to store ONLY the configuration data of each trial - not the data gathered from actual experimentation
public record TrialConfig(String name, Pattern initialPattern, double trialTime, double restTime, double dimPercent, int maxTrials) {
    @Override
    public String toString() {
        return name;
    }
}