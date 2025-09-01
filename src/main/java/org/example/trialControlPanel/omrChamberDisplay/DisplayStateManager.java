package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DisplayStateManager {

    private final Core core;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> executorHandler;
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
        experiments = new ArrayList<>();
    }

    public int getCurExperimentIndex() {
        return currentExperimentIndex;
    }
    public Experiment getCurExperiment() {
        return experiments.get(currentExperimentIndex);
    }
    public int getCurTrial() {
        return currentTrial;
    }

    public void setTransitionFunction(DisplayState from, DisplayState to, Runnable function) {
        transitionFunctions.put(new Transition(from, to), function);
    }

    public void runExperiments(ArrayList<Experiment> experiments, long intervalNanos, int inBetweenExperimentsRestTime) {
        this.experiments = experiments;
        System.out.println(experiments);
        this.inBetweenExperimentsRestTime = inBetweenExperimentsRestTime;
        currentExperimentIndex = 0;
        currentTrial = 0;
        state = DisplayState.TESTING;
        transitionFunctions.getOrDefault(new Transition(DisplayState.SETUP, DisplayState.TESTING), () -> {}).run();
        final double startTimeMs = System.currentTimeMillis();
        lastExperimentFinishTimeMs = startTimeMs;
        lastTrialFinishTimeMs = startTimeMs;

        executorHandler = executor.scheduleAtFixedRate(() -> {
            totalSecondsRunning = (System.currentTimeMillis() - startTimeMs) / 1000.;
            currentExperimentSecondsRunning = (System.currentTimeMillis() - lastExperimentFinishTimeMs) / 1000.;
            currentTrialSecondsRunning = (System.currentTimeMillis() - lastTrialFinishTimeMs) / 1000.;

            switch (state) {
                case SETUP:
                    break;
                case TESTING:
                    if (currentExperimentSecondsRunning >= getCurExperiment().getTotalTime() - getCurExperiment().getRestTime() && currentExperimentIndex + 1 >= experiments.size()) {
                        transitionFunctions.getOrDefault(new Transition(DisplayState.TESTING, DisplayState.NORMAL_STOP), () -> {}).run();
                        stopRunning(true);
                    }
                    else if (currentTrialSecondsRunning >= getCurExperiment().getTestTime()) {
                        state = DisplayState.RESTING;
                        transitionFunctions.getOrDefault(new Transition(DisplayState.TESTING, DisplayState.RESTING), () -> {}).run();
                    }
                    break;
                case RESTING:
                    if (currentExperimentSecondsRunning >= getCurExperiment().getTotalTime()) {
                        state = DisplayState.IN_BETWEEN_EXPERIMENTS;
                        transitionFunctions.getOrDefault(new Transition(DisplayState.RESTING, DisplayState.IN_BETWEEN_EXPERIMENTS), () -> {}).run();
                    }
                    else if (currentTrialSecondsRunning >= getCurExperiment().getCycleTime()) {
                        currentTrial++;
                        lastTrialFinishTimeMs = System.currentTimeMillis();
                        state = DisplayState.TESTING;
                        transitionFunctions.getOrDefault(new Transition(DisplayState.RESTING, DisplayState.TESTING), () -> {}).run();
                    }
                    break;
                case IN_BETWEEN_EXPERIMENTS:
                    if (currentExperimentSecondsRunning >= getCurExperiment().getTotalTime() + inBetweenExperimentsRestTime) {
                        state = DisplayState.TESTING;
                        lastExperimentFinishTimeMs = System.currentTimeMillis();
                        lastTrialFinishTimeMs = System.currentTimeMillis();
                        currentExperimentIndex++;
                        currentTrial = 0;
                        transitionFunctions.getOrDefault(new Transition(DisplayState.IN_BETWEEN_EXPERIMENTS, DisplayState.TESTING), () -> {}).run();
                    }
                    break;
            }
            Platform.runLater(core.getRunTrialController()::updateUILabels);

        }, 0, intervalNanos, TimeUnit.NANOSECONDS);
    }

    public void stopRunning(boolean normalStop) {
        state = normalStop ? DisplayState.NORMAL_STOP : DisplayState.EARLY_STOP;
        if (executorHandler != null && !executorHandler.isCancelled())
        executorHandler.cancel(true);
    }
    public void shutDownExecutor() {
        executor.shutdown();
    }

    // run trial controller getters
    public DisplayState getState() {
        return state;
    }
    public int getNumExperiments() {
        return experiments.size();
    }
    public double getTotalTime() {
        return experiments.stream()
                .mapToInt(Experiment::getTotalTime)
                .sum() + (experiments.size() - 1) * inBetweenExperimentsRestTime - experiments.getLast().getRestTime();
    }
    public double getTotalSecondsRunning() {
        return totalSecondsRunning;
    }
    public double getTestRunTime() {
        return state == DisplayState.TESTING ? currentTrialSecondsRunning : 0;
    }
    public double getRestRunTime() {
        return state == DisplayState.RESTING ? currentTrialSecondsRunning - getCurExperiment().getTestTime() : 0;
    }
}
