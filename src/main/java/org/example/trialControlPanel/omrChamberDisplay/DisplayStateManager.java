package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import org.example.cameraCode.CameraManager;
import org.example.cameraCode.VisualizedImageReader;
import org.example.trialControlPanel.parentClasses.Core;
import org.example.trialControlPanel.trialConfig.Experiment;
import org.opencv.core.Mat;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.HashMap;

public class DisplayStateManager {

    private long updateIntervalMs;
    private boolean keepUpdating;
    private final Core core;
    private ArrayList<Experiment> experiments;
    private int currentExperimentIndex;
    private int inBetweenExperimentsRestTime;
    private double experimentQueueStartTimeMs;
    private double currentStateStartTimeMs;
    private int currentTrial;
    private DisplayState state;
    private final HashMap<Transition, Runnable> transitionFunctions;
    private final HashMap<DisplayState, Runnable> updateFunctions;
    private final VisualizedImageReader visualizedImageReader;
    public boolean canSaveResults = false;

    public DisplayStateManager(Core core, VisualizedImageReader visualizedImageReader) {
        this.core = core;
        this.visualizedImageReader = visualizedImageReader;
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
    public ArrayList<Experiment> getExperiments() {
        return experiments;
    }
    public int getCurTrialIndex() {
        return currentTrial;
    }
    public double getCurStateTime() {
        return (System.currentTimeMillis() - currentStateStartTimeMs) / 1000.;
    }
    private void setNewState(DisplayState newState) {
        DisplayState oldState = this.state;
        this.state = newState;
        currentStateStartTimeMs = System.currentTimeMillis();
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
        this.updateIntervalMs = intervalNanos / 1_000_000;
        this.inBetweenExperimentsRestTime = inBetweenExperimentsRestTime;
        currentExperimentIndex = 0;
        currentTrial = 0;
        state = DisplayState.TESTING;
        experimentQueueStartTimeMs = System.currentTimeMillis();
        currentStateStartTimeMs = experimentQueueStartTimeMs;

        keepUpdating = true;
        Thread updateThread = new Thread(() -> {
            while (keepUpdating) {
                update();
                try {
                    Thread.sleep(updateIntervalMs);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, "update display SM thread");
        updateThread.start();
    }

    private void update() {
        updateState(); // update any logic that the OMR Chamber Controller wants to update during the current DisplayState

        switch (state) {
            case NORMAL_STOP:
                if (imagesAreCompletelySaved()) {
                    addDataFromMostRecentTrialToResultsController();
                    core.getStartMenuController().finishExperiment(getCurExperiment(), core.getOmrChamberController().patternDrawer.getPatternData());
                    stopUpdating();
                    canSaveResults = true;
                }
                break;
            case TESTING:
                if (getCurStateTime() >= getCurExperiment().getTestTime()) {
                    if (getCurTrialIndex() + 1 >= getCurExperiment().getMaxTests() && getCurExperimentIndex() + 1 >= experiments.size()) {
                        setNewState(DisplayState.NORMAL_STOP);

                        Platform.runLater(() -> core.getOmrChamberController().stopShowingExperiments(false));
                        System.out.println("testing -> normal stop transition function called");
                    }
                    else
                        setNewState(DisplayState.RESTING);
                }
                break;
            case RESTING:
                if (getCurStateTime() >= getCurExperiment().getRestTime()) {
                    if (imagesAreCompletelySaved()) {
                        addDataFromMostRecentTrialToResultsController();

                        if (getCurTrialIndex() + 1 >= getCurExperiment().getMaxTests()) {
                            core.getStartMenuController().finishExperiment(getCurExperiment(), core.getOmrChamberController().patternDrawer.getPatternData());
                            setNewState(DisplayState.IN_BETWEEN_EXPERIMENTS);
                        }
                        else {
                            waitForCameraToReachStableFPS();

                            currentTrial++;
                            setNewState(DisplayState.TESTING);
                        }
                    }
                }
                break;
            case IN_BETWEEN_EXPERIMENTS:
                if (getCurStateTime() > inBetweenExperimentsRestTime
                        && core.getCameraManager().getSendState() == CameraManager.SendState.FINISHED_SENDING_IMAGES) {
                    waitForCameraToReachStableFPS();

                    currentExperimentIndex++;
                    currentTrial = 0;
                    setNewState(DisplayState.TESTING);
                }
                break;
        }
        Platform.runLater(core.getRunTrialController()::updateUILabels);
    }

    private boolean imagesAreCompletelySaved() {
        return core.getCameraManager().getSendState() == CameraManager.SendState.FINISHED_SENDING_IMAGES
                && visualizedImageReader.getState() == VisualizedImageReader.State.FINISHED_RECEIVING_IMAGES;
    }

    public void stopUpdating() {
        keepUpdating = false;
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
        return (System.currentTimeMillis() - experimentQueueStartTimeMs) * 0.001;
    }
    public double getTestRunTime() {
        return state == DisplayState.TESTING ? (System.currentTimeMillis() - currentStateStartTimeMs) * 0.001 : 0;
    }
    public double getRestRunTime() {
        return state == DisplayState.RESTING ? (System.currentTimeMillis() - currentStateStartTimeMs) * 0.001 : 0;
    }
    private void waitForCameraToReachStableFPS() {
        System.out.println("STARTING DISPLAY SM WAIT FOR STABLE FPS THREAD");
        // block displaySM thread until stable FPS is reached
        core.getCameraManager().getImageGrabber().startGrabbing();
        Thread waitForStableFPSThread = new Thread(() -> {
            long unstableFPSSleepTimeMs = 1000 / 30;
            while (!core.getCameraManager().getImageGrabber().reachedStableFPS()) {
                try {
                    Thread.sleep(unstableFPSSleepTimeMs);
                } catch (InterruptedException ignored) {
                }
                System.out.println("RUNNING DISPLAY SM WAIT FOR STABLE FPS THREAD");
            }
        }, "wait for stable FPS thread");
        waitForStableFPSThread.start();
    }

    private void addDataFromMostRecentTrialToResultsController() {
        System.out.println("adding trial info to results controller");

        ArrayList<Mat> rawImages = core.getCameraManager().getRawImagesSoFar();
        ArrayList<RenderedImage> visualizeImages = visualizedImageReader.getRenderedImagesSoFar();
        double[] result = VisualizedImageReader.getOMRInfo(visualizedImageReader.omrDetected, core.getCameraManager().timeStampsMs);
        core.getStartMenuController().addTrialResult(rawImages, visualizeImages, result, core.getCameraManager().timeStampsMs, visualizedImageReader.headAngles, visualizedImageReader.tailAngles);
    }
}
