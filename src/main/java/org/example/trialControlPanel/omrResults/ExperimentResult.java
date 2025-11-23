package org.example.trialControlPanel.omrResults;

import org.example.trialControlPanel.trialConfig.Experiment;

import java.util.ArrayList;

public class ExperimentResult {
    public final Experiment experiment;
    private final ArrayList<Integer> numOMR;
    public ExperimentResult(Experiment experiment, ArrayList<Integer> numOMR) {
        this.experiment = experiment;
        this.numOMR = numOMR;
    }

    public int getNumOMRFromTrial(int trialIndex) {
        return numOMR.get(trialIndex);
    }
    public int getNumTrials() {
        return numOMR.size();
    }
}
