package org.example.patternControlPanel.trialDataManager;

import org.example.patternControlPanel.pattern.Pattern;
import org.example.patternControlPanel.pattern.PatternDirection;
import org.example.patternControlPanel.trialConfig.TrialConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrialDataManager {
    public static final TrialConfig DEFAULT_TRIAL = new TrialConfig("defaultTrial", new Pattern("", PatternDirection.CLOCKWISE, 1, 1, 0, 1), 1, 1, 1, 1);
    private static final ArrayList<TrialConfig> savedTrials = new ArrayList<>(List.of(DEFAULT_TRIAL));

    public static ArrayList<TrialConfig> getSavedTrials() {
        return savedTrials;
    }
    public static void removeSavedTrial(String trialConfigName) {
        for (int i=0; i<savedTrials.size(); i++)
            if (savedTrials.get(i).name().equals(trialConfigName)) {
                savedTrials.remove(i);
                return;
            }
    }
    public static void loadSavedTrials(String filePath) {

    }
    public static void saveTrials(String filePath) {
    }
}
