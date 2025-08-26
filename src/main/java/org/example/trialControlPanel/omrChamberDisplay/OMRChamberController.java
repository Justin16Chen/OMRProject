package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.TrialConfig;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
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

	private DataInputStream visualizedImageDataIn;
	private Image visualizedImg;

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

		// connect datastream to python socket (python code sends visualized images to java code - java code displays the images)
		try (ServerSocket serverSocket = new ServerSocket(getCore().getProgramInfoWriter().getSocketPort())) {
			Socket clientSocket = serverSocket.accept();
			visualizedImageDataIn = new DataInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// manage trial logic on separate thread
		new Thread(() -> {
			double startTimeMs = System.currentTimeMillis();
			double lastTrialFinishTimeMs = startTimeMs;
			double lastCycleFinishTimeMs = startTimeMs;

			// create proper file structure (experiment folder -> trial folder -> images)
			// do this 2x (for camera images and visualized image)
			for (int i=0; i<trials.size(); i++) {
				TrialConfig experiment = trials.get(i);
				int experimentNum = i + 1;
				boolean ec = getCameraImageExperimentFolder(experiment.getName(), experimentNum).mkdir();
				boolean tc = getCameraImageTrialFolder(experiment.getName(), experimentNum, 1).mkdir();
				boolean ev = getVisualizedImageExperimentFolder(experiment.getName(), experimentNum).mkdir();
				boolean tv = getVisualizedImageTrialFolder(experiment.getName(), experimentNum, 1).mkdir();
				System.out.println("FOLDER CREATION SUCCESS");
				System.out.println(ec + " " + tc + " " + ev + " " + tv);
			}

			// specify where the camera should save the raw images
			getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(trials.getFirst().getName(), 1, 1).getPath());

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
					else {
						try {
							byte[] imgBytes = new byte[visualizedImageDataIn.readInt()];
							visualizedImageDataIn.readFully(imgBytes);
							ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
							visualizedImg = new Image(bais);
						} catch (IOException e) {
							e.printStackTrace();
						}
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
						getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(currentTrial.getName(), currentTrialIndex + 1, currentCycle + 1).getPath());
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
						getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(currentTrial.getName(), currentTrialIndex + 1, 1).getPath());
					}
				}

				Platform.runLater(getCore().getRunTrialController()::updateUILabels);
				Platform.runLater(() -> getCore().getRunTrialController().updateCameraImageView(visualizedImg));

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

	// folder structure/file helper functions
	private File getCameraImageExperimentFolder(String experimentName, int experimentNum) {
        return Paths.get(getCore().getStartMenuController().getCameraOutputPath(), experimentNum + " - " + experimentName).toFile();
	}
	private File getCameraImageTrialFolder(String experimentName, int experimentNum, int trialNum) {
		return Paths.get(getCameraImageExperimentFolder(experimentName, experimentNum).getPath(), "trial" + trialNum).toFile();
	}
	private File getVisualizedImageExperimentFolder(String experimentName, int experimentNum) {
        return Paths.get(getCore().getStartMenuController().getVisualizedOutputPath(), experimentNum + " - " + experimentName).toFile();
	}
	private File getVisualizedImageTrialFolder(String experimentName, int experimentNum, int trialNum) {
		return Paths.get(getVisualizedImageExperimentFolder(experimentName, experimentNum).getPath(), "trial" + trialNum).toFile();
	}


	// getters
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
}
