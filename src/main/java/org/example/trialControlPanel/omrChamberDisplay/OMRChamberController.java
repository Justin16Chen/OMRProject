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

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OMRChamberController extends CustomController {
	public enum State {
		TESTING, RESTING, IN_BETWEEN_TRIALS
	}
	private ArrayList<TrialConfig> trials;
	private int currentTrialIndex;
	private int inBetweenTrialsRestTime;
	private double lastTrialFinishTimeMs, lastCycleFinishTimeMs;
	public int getCurrentTrialIndex() {
		return currentTrialIndex;
	}
	private PatternDrawer patternDrawer;
	private ScheduledExecutorService[] executors;
	private int lastActualImageIndex;
	private double totalSecondsRunning, currentTrialSecondsRunning, currentCycleSecondsRunning; // 1 cycle = 1 test and 1 rest
	public double getTotalSecondsRunning() {
		return totalSecondsRunning;
	}
	public double getTotalTime() {
		return trials.stream()
				.mapToInt(TrialConfig::getTotalTime)
				.sum() + (trials.size() - 1) * inBetweenTrialsRestTime - trials.getLast().getRestTime();
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
		patternDrawer.stop();
		Platform.runLater(getStage()::close);
		for (ChildOMRController child : getCore().getChildOMRControllers()) {
			Platform.runLater(() -> {
				child.getStage().close();
				child.getPatternDrawer().stop();
			});
		}

		for (ScheduledExecutorService executor : executors)
			executor.shutdown();

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
		getCore().getProgramInfoWriter().activateExperiments(trials);

		state = State.TESTING;
		patternDrawer.start();
		for (ChildOMRController child : getCore().getChildOMRControllers())
			child.getPatternDrawer().start();

		currentTrialIndex = 0;
		currentCycle = 0;
		CameraManager cm = getCore().getCameraManager();
		cm.startRecording();
		cm.setSaveImage(true);
		cm.setImageIndexCap(getCore().getProgramInfoWriter().getExpectedImages(0, trials.getFirst().getName()) - 1);

		// connect datastream to python socket (python code sends visualized images to java code - java code displays the images)
		try (ServerSocket serverSocket = new ServerSocket(getCore().getProgramInfoWriter().getSocketPort())) {
			Socket clientSocket = serverSocket.accept();
			visualizedImageDataIn = new DataInputStream(clientSocket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// create independent executors
		executors = new ScheduledExecutorService[3];
		for (int i=0; i<executors.length; i++)
			executors[i] = Executors.newSingleThreadScheduledExecutor();

		// declare executor intervals
		long cameraIntervalNanos = 1_000_000_000L / getCore().getProgramInfoWriter().getFPS();

		// create proper file structure (experiment folder -> trial folder -> images)
		// do this 2x (for camera images and visualized image)
		for (int i=0; i<trials.size(); i++) {
			TrialConfig experiment = trials.get(i);
			boolean ec = getCameraImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tc = getCameraImageTrialFolder(experiment.getName(), i, 0).mkdir();
			boolean ev = getVisualizedImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tv = getVisualizedImageTrialFolder(experiment.getName(), i, 0).mkdir();
//				System.out.println("FOLDER CREATION SUCCESS");
//				System.out.println(ec + " " + tc + " " + ev + " " + tv);
		}

		// specify where the camera should save the raw images
		getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(trials.getFirst().getName(), 0, 0).getPath());

		// set timestamps used to manage trial test/rest
		final double startTimeMs = System.currentTimeMillis();
		lastTrialFinishTimeMs = startTimeMs;
		lastCycleFinishTimeMs = startTimeMs;

		// manage trial logic on separate thread
		executors[0].scheduleAtFixedRate(() -> {
			totalSecondsRunning = (System.currentTimeMillis() - startTimeMs) / 1000.;
			currentTrialSecondsRunning = (System.currentTimeMillis() - lastTrialFinishTimeMs) / 1000.;
			currentCycleSecondsRunning = (System.currentTimeMillis() - lastCycleFinishTimeMs) / 1000.;

			TrialConfig currentTrial = trials.get(currentTrialIndex);

			if (state == State.TESTING) {
				if (currentTrialSecondsRunning >= currentTrial.getTotalTime() - currentTrial.getRestTime() && currentTrialIndex + 1 >= trials.size()) {
					lastActualImageIndex = cm.getImageIndex() - 1;
					if (lastActualImageIndex <= cm.getImageIndexCap()) {
						writeTrialCameraInfoFile(currentTrial.getName(), currentTrialIndex, currentCycle);
						cm.fillImagesToCap();
					}
					Platform.runLater(() -> this.stopTrial(false));
				}
				else if (currentCycleSecondsRunning >= currentTrial.getTestTime()) {
					state = State.RESTING;
					getCore().getProgramInfoWriter().completeExperiment(currentTrialIndex, currentTrial.getName());

					patternDrawer.stopAndBlackOutScreen();
					for (ChildOMRController child : getCore().getChildOMRControllers())
						child.getPatternDrawer().stopAndBlackOutScreen();

					cm.setSaveImage(false);
					if (cm.getImageIndex() <= cm.getImageIndexCap()) {
						lastActualImageIndex = cm.getImageIndex() - 1;
						writeTrialCameraInfoFile(currentTrial.getName(), currentTrialIndex, currentCycle);
						cm.fillImagesToCap();
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
					getCameraImageTrialFolder(currentTrial.getName(), currentTrialIndex, currentCycle).mkdir();
					getCore().getCameraManager().resetImageIndex();
					getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(currentTrial.getName(), currentTrialIndex, currentCycle).getPath());
				}
			}
			else if (state == State.IN_BETWEEN_TRIALS) {
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
					getCore().getCameraManager().resetImageIndex();
					cm.setImageIndexCap(getCore().getProgramInfoWriter().getExpectedImages(currentTrialIndex, currentTrial.getName()) - 1);
					getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(currentTrial.getName(), currentTrialIndex, 0).getPath());
				}
			}

			Platform.runLater(getCore().getRunTrialController()::updateUILabels);
		}, 0, cameraIntervalNanos, TimeUnit.NANOSECONDS);

		// manage reading visualized images from python on separate thread so python lag does not block java program
		executors[1].scheduleAtFixedRate(() -> {
			try {
				byte[] imgBytes = new byte[visualizedImageDataIn.readInt()];
				System.out.println("image reading thread started");
				visualizedImageDataIn.readFully(imgBytes);
				ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
				visualizedImg = new Image(bais);
				Platform.runLater(() -> getCore().getRunTrialController().updateCameraImageView(visualizedImg));
				System.out.println("image read and command sent to image view");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, 0, cameraIntervalNanos, TimeUnit.NANOSECONDS);

		// manage camera on separate thread for custom FPS
		executors[2].scheduleAtFixedRate(() -> {
			try {
				cm.update();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, cameraIntervalNanos, TimeUnit.NANOSECONDS);
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

	private void writeTrialCameraInfoFile(String experimentName, int experimentNum, int trialNum) {
		File file = Paths.get(getCameraImageTrialFolder(experimentName, experimentNum, trialNum).getPath(), "duplicatedImages.txt").toFile();

		try {
			file.createNewFile();

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				writer.write("lastActualFile: " + lastActualImageIndex + ".png");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
