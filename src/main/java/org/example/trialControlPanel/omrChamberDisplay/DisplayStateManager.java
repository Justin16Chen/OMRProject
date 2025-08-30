package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DisplayStateManager {

    private final Core core;
    private final ScheduledExecutorService executor;
    private ArrayList<Experiment> experiments;
    private int currentExperimentIndex;
    private int inBetweenExperimentsRestTime;
    private double lastExperimentFinishTimeMs, lastTrialFinishTimeMs;
    private double totalSecondsRunning, currentExperimentSecondsRunning, currentTrialSecondsRunning;
    private int currentTrial;
    private DisplayState state;
    private final HashMap<Transition, Runnable> transitionFunctions;

    public DisplayStateManager(Core core) {
        this.core = core;
        executor = Executors.newSingleThreadScheduledExecutor();
        transitionFunctions = new HashMap<>();
    }

    public void setInBetweenExperimentsRestTime(int restTime) {
        this.inBetweenExperimentsRestTime = restTime;
    }
    public DisplayState getState() {
        return state;
    }
    private Experiment getCurExperiment() {
        return experiments.get(currentExperimentIndex);
    }

    public void setTransitionFunction(DisplayState from, DisplayState to, Runnable function) {
        transitionFunctions.put(new Transition(from, to), function);
    }

    public void runExperiments(ArrayList<Experiment> experiments, long intervalNanos) {
        this.experiments = experiments;
        currentExperimentIndex = 0;
        currentTrial = 0;
        state = DisplayState.TESTING;
        final double startTimeMs = System.currentTimeMillis();

        executor.scheduleAtFixedRate(() -> {
            totalSecondsRunning = (System.currentTimeMillis() - startTimeMs) / 1000.;
            currentExperimentSecondsRunning = (System.currentTimeMillis() - lastExperimentFinishTimeMs) / 1000.;
            currentTrialSecondsRunning = (System.currentTimeMillis() - lastTrialFinishTimeMs) / 1000.;

            switch (state) {
                case SETUP:
                    break;
                case TESTING:
                    if (currentExperimentSecondsRunning >= getCurExperiment().getTotalTime() - getCurExperiment().getRestTime() && currentExperimentIndex + 1 >= experiments.size()) {
                        transitionFunctions.getOrDefault(new Transition(DisplayState.TESTING, DisplayState.STOP), () -> {}).run();
                        stopRunning();
                    }
                    else if (currentTrialSecondsRunning >= getCurExperiment().getTestTime()) {
                        transitionFunctions.getOrDefault(new Transition(DisplayState.TESTING, DisplayState.RESTING), () -> {}).run();
                        state = DisplayState.RESTING;
                    }
                    break;
                case RESTING:
                    if (currentExperimentSecondsRunning >= getCurExperiment().getTotalTime()) {
                        transitionFunctions.getOrDefault(new Transition(DisplayState.RESTING, DisplayState.IN_BETWEEN_EXPERIMENTS), () -> {}).run();
                        state = DisplayState.IN_BETWEEN_EXPERIMENTS;
                    }
                    else if (currentTrialSecondsRunning >= getCurExperiment().getCycleTime()) {
                        transitionFunctions.getOrDefault(new Transition(DisplayState.RESTING, DisplayState.TESTING), () -> {}).run();
                        state = DisplayState.TESTING;
                        lastTrialFinishTimeMs = System.currentTimeMillis();
                        currentTrial++;
                    }
                    break;
                case IN_BETWEEN_EXPERIMENTS:
                    if (currentExperimentSecondsRunning >= getCurExperiment().getTotalTime() + inBetweenExperimentsRestTime) {
                        transitionFunctions.getOrDefault(new Transition(DisplayState.IN_BETWEEN_EXPERIMENTS, DisplayState.TESTING), () -> {}).run();
                        state = DisplayState.TESTING;
                        lastExperimentFinishTimeMs = System.currentTimeMillis();
                        lastTrialFinishTimeMs = System.currentTimeMillis();
                        currentTrial = 0;
                    }
                    break;
            }
            Platform.runLater(core.getRunTrialController()::updateUILabels);

        }, 0, intervalNanos, TimeUnit.NANOSECONDS);
    }

    public void stopRunning() {
        state = DisplayState.STOP;
        executor.shutdown();
    }
}
