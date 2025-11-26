package org.example.trialControlPanel.omrResults;

import org.example.trialControlPanel.pattern.Pattern;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.util.ArrayList;

public class ExperimentResult {
    public Experiment experiment;
    public ArrayList<TrialResult> trialResults = new ArrayList<>();
    public Pattern endingPattern;

    public int getNumTrials() {
        return trialResults.size();
    }
    public int getNumOMR(int trialIndex) {
        return (int) trialResults.get(trialIndex).results()[0];
    }
    public double getAverageDuration(int trialIndex) {
        return trialResults.get(trialIndex).results()[1];
    }
    public double getMedian(int trialIndex) {
        return trialResults.get(trialIndex).results()[2];
    }
}
