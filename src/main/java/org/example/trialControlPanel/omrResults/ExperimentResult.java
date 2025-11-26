package org.example.trialControlPanel.omrResults;

import org.example.trialControlPanel.pattern.Pattern;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.util.ArrayList;

public class ExperimentResult {
    public final Experiment experiment;
    private final ArrayList<double[]> results;
    public final Pattern endingPattern;
    public ExperimentResult(Experiment experiment, ArrayList<double[]> results, Pattern endingPattern) {
        this.experiment = experiment;
        this.results = results;
        this.endingPattern = endingPattern;
    }

    public int getNumTrials() {
        return results.size();
    }
    public int getNumOMR(int trialIndex) {
        return (int) results.get(trialIndex)[0];
    }
    public double getAverageDuration(int trialIndex) {
        return results.get(trialIndex)[1];
    }
    public double getMedian(int trialIndex) {
        return results.get(trialIndex)[2];
    }
}
