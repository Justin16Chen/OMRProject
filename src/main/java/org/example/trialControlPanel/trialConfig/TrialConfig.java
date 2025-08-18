package org.example.trialControlPanel.trialConfig;

import org.example.trialControlPanel.pattern.Pattern;

// meant to store ONLY the configuration data of each trial - not the data gathered from actual experimentation
public class TrialConfig {
    private String name;
    private final Pattern initialPattern;
    private int testTime;
    private int restTime;
    private int dimAmount;
    private int maxTests;

    public TrialConfig(String name, Pattern initialPattern, int testTime, int restTime, int dimAmount, int maxTests) {
        this.name = name;
        this.initialPattern = initialPattern;
        this.testTime = testTime;
        this.restTime = restTime;
        this.dimAmount = dimAmount;
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

    public int getTestTime() {
        return testTime;
    }
    public void setTestTime(int time) {
        this.testTime = time;
    }

    public int getRestTime() {
        return restTime;
    }
    public void setRestTime(int time) {
        this.restTime = time;
    }
    public int getCycleTime() {
        return restTime + testTime;
    }

    public int getDimAmount() {
        return dimAmount;
    }
    public void setDimAmount(int amount) {
        this.dimAmount = amount;
    }

    public int getMaxTests() {
        return maxTests;
    }
    public void setMaxTests(int maxTrials) {
        this.maxTests = maxTrials;
    }

    public int getTotalTime() {
        return maxTests * (testTime + restTime);
    }

    @Override
    public String toString() {
        return name;
    }

    public TrialConfig deepCopy() {
        return new TrialConfig(
                name,
                initialPattern,
                testTime,
                restTime,
                dimAmount,
                maxTests
        );
    }
}
