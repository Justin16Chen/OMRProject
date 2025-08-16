package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.TrialConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class OMRChamberController extends CustomController {

	private static final int CAMERA_RECORD_FPS = 24;

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
				stopTrial();
			}
		});
	}

	public void stopTrial() {;
		patternDrawer.stop();
		trialRunning = false;
		Platform.runLater(() -> getStage().close());
		getCore().getCameraManager().stopRecording();
		getCore().getRunTrialController().getStage().close();
	}

	@FXML
	private Canvas canvas;

	public void initPatternDrawer(MonitorFormat monitorFormat, ArrayList<TrialConfig> trials, int restTime) {
		this.trials = trials;
		patternDrawer = new PatternDrawer(monitorFormat, trials.getFirst().getInitialPattern(), canvas, SimulatedSurface.CIRCULAR);
		inBetweenTrialsRestTime = restTime;
	}

	public void startTrials() {
		int sleepInterval = 1000 / CAMERA_RECORD_FPS;

		trialRunning = true;
		state = State.TESTING;
		patternDrawer.start();
		currentTrialIndex = 0;
		currentCycle = 0;
		CameraManager cm = getCore().getCameraManager();
		cm.startRecording();
		cm.setSavePermanentImages(true);

		// manage trials on separate thread
		new Thread(() -> {
			double startTimeMs = System.currentTimeMillis();
			double lastTrialFinishTimeMs = startTimeMs;
			double lastCycleFinishTimeMs = startTimeMs;

			while (trialRunning) {
				totalSecondsRunning = (System.currentTimeMillis() - startTimeMs) / 1000.;
				currentTrialSecondsRunning = (System.currentTimeMillis() - lastTrialFinishTimeMs) / 1000.;
				currentCycleSecondsRunning = (System.currentTimeMillis() - lastCycleFinishTimeMs) / 1000.;

				if (state == State.TESTING) {
					if (currentTrialSecondsRunning >= trials.get(currentTrialIndex).getTotalTime()) {
						System.out.println("trial " + currentTrialIndex + "/" + trials.size() + " finished");
						if (currentTrialIndex + 1 >= trials.size())
							Platform.runLater(this::stopTrial);
						else {
							state = State.IN_BETWEEN_TRIALS;
							patternDrawer.stop();
							Platform.runLater(patternDrawer::showBlank);
							cm.setSavePermanentImages(false);
						}
					}
					else if (currentCycleSecondsRunning > trials.get(currentTrialIndex).getTestTime()) {
						state = State.RESTING;
						patternDrawer.stop();
						Platform.runLater(patternDrawer::showBlank);
						cm.setSavePermanentImages(false);
					}
				}
				else if (state == State.RESTING) {
					if (currentCycleSecondsRunning > trials.get(currentTrialIndex).getCycleTime()) {
						state = State.TESTING;
						lastCycleFinishTimeMs = System.currentTimeMillis();
						patternDrawer.start();
						patternDrawer.getPatternData().setLightBrightness(patternDrawer.getPatternData().getLightBrightness() - trials.get(currentTrialIndex).getDimAmount());
						currentCycle++;
						cm.setSavePermanentImages(true);
					}
				} else if (state == State.IN_BETWEEN_TRIALS) {
					if (currentTrialSecondsRunning >= trials.get(currentTrialIndex).getTotalTime() + inBetweenTrialsRestTime) {
						state = State.TESTING;
						currentTrialIndex++;
						lastTrialFinishTimeMs = System.currentTimeMillis();
						currentCycle = 0;
						patternDrawer.setPatternData(trials.get(currentTrialIndex).getInitialPattern());
					}
				}

				cm.update(); // update camera manager

				Platform.runLater(getCore().getRunTrialController()::updateUILabels);
				Platform.runLater(() -> getCore().getRunTrialController().updateCameraImageView(cm.getLatestImage()));

				try {
					Thread.sleep(sleepInterval);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
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
