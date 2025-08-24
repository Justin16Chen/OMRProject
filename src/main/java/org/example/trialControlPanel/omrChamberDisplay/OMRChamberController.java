package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import org.example.cameraCode.CameraManager;
import org.example.integration.ProgramInfoManager;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.TrialConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class OMRChamberController extends CustomController {

	private static final int PATTERN_FPS = 60;
	public enum State {
		TESTING, RESTING, IN_BETWEEN_TRIALS
	}
	private ArrayList<TrialConfig> trials;
	private int currentTrialIndex;
	private int inBetweenTrialsRestTime;
	public int getCurrentTrialIndex() {
		return currentTrialIndex;
	}
	private PatternDrawer patternDrawer;
	private boolean trialRunning;
	private double totalSecondsRunning, currentTrialSecondsRunning, currentCycleSecondsRunning; // 1 cycle = 1 test and 1 rest
	public double getTotalSecondsRunning() {
		return totalSecondsRunning;
	}
	public double getTotalTime() {
		return trials.stream()
				.mapToInt(TrialConfig::getTotalTime)
				.sum() + (trials.size() - 1) * inBetweenTrialsRestTime;
	}
	private int currentCycle;
	public int getCurrentCycle() {
		return currentCycle;
	}
	private State state;
	public State getState() {
		return state;
	}

	@Override
	public void setup() {
		getCore().getOMRChamberScene().setOnKeyPressed(e -> {
			if (Objects.requireNonNull(e.getCode()) == KeyCode.ESCAPE) {
				stopTrial(true);
			}
		});
	}

	public void stopTrial(boolean earlyStop) {;
		trialRunning = false;
		patternDrawer.stop();
		Platform.runLater(getStage()::close);
		for (ChildOMRController child : getCore().getChildOMRControllers()) {
			Platform.runLater(() -> {
				child.getStage().close();
				child.getPatternDrawer().stop();
			});
		}

		getCore().getCameraManager().stopRecording();
		getCore().getRunTrialController().getStage().close();
		if (earlyStop)
			getCore().getProgramInfoWriter().stopExperimentsEarly();
		else
			getCore().getProgramInfoWriter().completeAllExperiments();
	}

	@FXML
	private Canvas canvas;

	public void initPatternDrawer(MonitorFormat monitorFormat, ArrayList<TrialConfig> trials, int restTime) {
		this.trials = trials;
		patternDrawer = new PatternDrawer(monitorFormat, trials.getFirst().getInitialPattern(), canvas, SimulatedSurface.CIRCULAR);
		inBetweenTrialsRestTime = restTime;
	}

	public void startTrials() {
		int patternSleepInterval = 1000 / PATTERN_FPS;
		getCore().getProgramInfoWriter().activateExperiments(trials);

		trialRunning = true;
		state = State.TESTING;
		patternDrawer.start();
		for (ChildOMRController child : getCore().getChildOMRControllers())
			child.getPatternDrawer().start();

		currentTrialIndex = 0;
		currentCycle = 0;
		CameraManager cm = getCore().getCameraManager();
		cm.startRecording();
		cm.setSaveImage(true);

		// manage trial logic on separate thread
		new Thread(() -> {
			double startTimeMs = System.currentTimeMillis();
			double lastTrialFinishTimeMs = startTimeMs;
			double lastCycleFinishTimeMs = startTimeMs;

			while (trialRunning) {
				totalSecondsRunning = (System.currentTimeMillis() - startTimeMs) / 1000.;
				currentTrialSecondsRunning = (System.currentTimeMillis() - lastTrialFinishTimeMs) / 1000.;
				currentCycleSecondsRunning = (System.currentTimeMillis() - lastCycleFinishTimeMs) / 1000.;

				TrialConfig currentTrial = trials.get(currentTrialIndex);

				if (state == State.TESTING) {
					if (currentTrialSecondsRunning >= currentTrial.getTotalTime() - currentTrial.getRestTime() && currentTrialIndex + 1 >= trials.size())
						Platform.runLater(() -> this.stopTrial(false));
					else if (currentCycleSecondsRunning >= currentTrial.getTestTime()) {
						state = State.RESTING;
						getCore().getProgramInfoWriter().completeExperiment(currentTrial.getName());

						patternDrawer.stopAndBlackOutScreen();
						for (ChildOMRController child : getCore().getChildOMRControllers())
							child.getPatternDrawer().stopAndBlackOutScreen();

						cm.setSaveImage(false);
					}
				}
				else if (state == State.RESTING) {
					if (currentTrialSecondsRunning >= currentTrial.getTotalTime()) {
						state = State.IN_BETWEEN_TRIALS;
					}
					else if (currentCycleSecondsRunning >= currentTrial.getCycleTime()) {
						state = State.TESTING;
						lastCycleFinishTimeMs = System.currentTimeMillis();
						patternDrawer.getPatternData().setLightBrightness(patternDrawer.getPatternData().getLightBrightness() - currentTrial.getDimAmount());
						patternDrawer.start();

						for (ChildOMRController child : getCore().getChildOMRControllers()) {
							child.getPatternDrawer().start();
							child.getPatternDrawer().getPatternData().setLightBrightness(patternDrawer.getPatternData().getLightBrightness() - currentTrial.getDimAmount());
						}
						currentCycle++;
						cm.setSaveImage(true);
					}
				} else if (state == State.IN_BETWEEN_TRIALS) {
					if (currentTrialSecondsRunning >= currentTrial.getTotalTime() + inBetweenTrialsRestTime) {
						state = State.TESTING;
						currentTrialIndex++;
						currentTrial = trials.get(currentTrialIndex);
						lastTrialFinishTimeMs = System.currentTimeMillis();
						lastCycleFinishTimeMs = System.currentTimeMillis();
						currentCycle = 0;
						patternDrawer.setPatternData(currentTrial.getInitialPattern());
						patternDrawer.start();
						for (ChildOMRController child : getCore().getChildOMRControllers()) {
							child.getPatternDrawer().setPatternData(currentTrial.getInitialPattern());
							child.getPatternDrawer().start();
						}
					}
				}

				Platform.runLater(getCore().getRunTrialController()::updateUILabels);
				Platform.runLater(() -> getCore().getRunTrialController().updateCameraImageView(cm.getLatestImage()));

				try {
					Thread.sleep(patternSleepInterval);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();

		// manage camera on separate thread for custom FPS
		int cameraSleepInterval = 1000 / getCore().getProgramInfoWriter().getFPS();
		new Thread(() -> {
			while (trialRunning) {
				try {
					cm.update();
					Thread.sleep(cameraSleepInterval);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
	}
	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}

	public double getTestRunTime() {
		return state == State.TESTING ? currentCycleSecondsRunning : 0;
	}
	public double getRestRunTime() {
		return state == State.RESTING ? currentCycleSecondsRunning - trials.get(currentTrialIndex).getTestTime() : 0;
	}

	private File getImageFile() {
		File folder = new File(CameraManager.RAW_IMAGES_PATH);
		File[] files = folder.listFiles(File::isFile); // only count files, not subfolders
		if (files == null)
			throw new IllegalStateException("folder at " + CameraManager.RAW_IMAGES_PATH + " is not found");
		if (files.length > 0) {
			int max = 0;
			File imageFile = files[0];
			for (File file : files) {
				String nameNumber = file.getName().substring(0, file.getName().length() - 4);
				max = Math.max(Integer.parseInt(nameNumber), max);
				imageFile = file;
			}
			return imageFile;
		}
		return null;
	}
}
