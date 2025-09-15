package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import org.example.cameraCode.CameraManager;
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
    private double rumExperimentsStartTime, currentStateStartTime;
    private int currentTrial;
    private DisplayState state;
    private final HashMap<Transition, Runnable> transitionFunctions;
    private final HashMap<DisplayState, Runnable> updateFunctions;

    public DisplayStateManager(Core core) {
        this.core = core;
        executor = Executors.newSingleThreadScheduledExecutor();
        transitionFunctions = new HashMap<>();
        updateFunctions = new HashMap<>();
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
    public double getCurStateTime() {
        return (System.currentTimeMillis() - currentStateStartTime) / 1000.;
    }
    private void setNewState(DisplayState newState) {
        DisplayState oldState = this.state;
        this.state = newState;
        currentStateStartTime = System.currentTimeMillis();
        transitionFunctions.getOrDefault(new Transition(oldState, newState), () -> {}).run();
        System.out.println("setNewState from " + oldState + " to " + newState);
    }

    public void setTransitionFunction(DisplayState from, DisplayState to, Runnable function) {
        transitionFunctions.put(new Transition(from, to), function);
    }
    public void setUpdateFunction(DisplayState state, Runnable function) {
        updateFunctions.put(state, function);
    }
    private void updateState() {
        updateFunctions.getOrDefault(state, () -> {}).run();
    }
    public void runExperiments(ArrayList<Experiment> experiments, long intervalNanos, int inBetweenExperimentsRestTime) {
        this.experiments = experiments;
        this.inBetweenExperimentsRestTime = inBetweenExperimentsRestTime;
        currentExperimentIndex = 0;
        currentTrial = 0;
        state = DisplayState.TESTING;
        currentStateStartTime = (double) System.currentTimeMillis();


        executorHandler = executor.scheduleAtFixedRate(() -> {
            updateState();
            switch (state) {
                case TESTING:
                    if (getCurStateTime() >= getCurExperiment().getTestTime()) {
                        if (getCurTrial() + 1 >= getCurExperiment().getMaxTests() && getCurExperimentIndex() + 1 >= experiments.size()) {
                            setNewState(DisplayState.NORMAL_STOP);
                        } else
                            setNewState(DisplayState.RESTING);
                    }
                    break;
                case RESTING:
                    if (getCurStateTime() >= getCurExperiment().getRestTime()) {
                        if (getCurTrial() + 1 >= getCurExperiment().getMaxTests()) {
                            setNewState(DisplayState.IN_BETWEEN_EXPERIMENTS);
                        }
                        else {
                            boolean canMoveOn = core.getCameraManager().getSendState() == CameraManager.State.READY
                                    && core.getCameraManager().getSaveState() == CameraManager.State.READY;
                            System.out.println("can go from resting to testing: " + canMoveOn);
                            if (canMoveOn) {
                                currentTrial++;
                                setNewState(DisplayState.TESTING);
                            }
                        }
                    }
                    break;
                case IN_BETWEEN_EXPERIMENTS:
                    if (getCurStateTime() > inBetweenExperimentsRestTime
                            && core.getCameraManager().getSendState() == CameraManager.State.READY
                            && core.getCameraManager().getSaveState() == CameraManager.State.READY) {
                        currentExperimentIndex++;
                        currentTrial = 0;
                        setNewState(DisplayState.TESTING);
                    }
                    break;
            }
            Platform.runLater(core.getRunTrialController()::updateUILabels);

        }, 0, intervalNanos, TimeUnit.NANOSECONDS);
    }

    public void stopExecutor() {
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
        return 0;
    }
    public double getTestRunTime() {
        return 0;
    }
    public double getRestRunTime() {
        return 0;
    }
}
