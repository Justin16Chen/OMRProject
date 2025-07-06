package org.example.patternControlPanel.trialConfig;

import org.example.patternControlPanel.pattern.Pattern;

// meant to store ONLY the configuration data of each trial - not the data gathered from actual experimentation
public class TrialConfig {
    private String name;
    private final Pattern initialPattern;
    private double testTime;
    private double restTime;
    private double dimPercent;
    private int maxTests;

    public TrialConfig(String name, Pattern initialPattern, double testTime, double restTime, double dimPercent, int maxTests) {
        this.name = name;
        this.initialPattern = initialPattern;
        this.testTime = testTime;
        this.restTime = restTime;
        this.dimPercent = dimPercent;
        this.maxTests = maxTests;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Pattern getInitialPattern() {
        return initialPattern;
    }

    public double getTestTime() {
        return testTime;
    }
    public void setTestTime(double time) {
        this.testTime = time;
    }

    public double getRestTime() {
        return restTime;
    }
    public void setRestTime(double time) {
        this.restTime = time;
    }

    public double getDimPercent() {
        return dimPercent;
    }
    public void setDimPercent(double percent) {
        this.dimPercent = percent;
    }

    public int getMaxTests() {
        return maxTests;
    }
    public void setMaxTests(int maxTrials) {
        this.maxTests = maxTrials;
    }

    @Override
    public String toString() {
        return name;
    }
}
