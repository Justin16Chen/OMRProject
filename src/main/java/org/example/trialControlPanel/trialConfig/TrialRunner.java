package org.example.trialControlPanel.trialConfig;

import java.util.ArrayList;

// meant to store the data gathered from actual experimentation
// meant to be used to help run trials
public class TrialRunner {
    private TrialConfig trialConfig;

    private ArrayList<Integer> omrCounts; // index 0 = first trial, omrCounts.get(0) = # times omr happened on first trial

    public TrialRunner(TrialConfig trialConfig) {
        this.trialConfig = trialConfig;
    }


}
