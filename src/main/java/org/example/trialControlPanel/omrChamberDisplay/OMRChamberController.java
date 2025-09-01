package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OMRChamberController extends CustomController {
	private PatternDrawer patternDrawer;
	private int lastActualImageIndex;

	private DisplayStateManager displaySM;
	private VisualizedImageReader visualizedImageReader;
	private ScheduledExecutorService cameraManagerExecutor;
	private ScheduledFuture<?> cameraManagerExecutorHandler;

	@Override
	public void setup() {
		displaySM = new DisplayStateManager(getCore());
		visualizedImageReader = new VisualizedImageReader(getCore());
		cameraManagerExecutor = Executors.newSingleThreadScheduledExecutor();

		declareDisplaySMTransitions();
	}
	private void declareDisplaySMTransitions() {
		displaySM.setTransitionFunction(DisplayState.TESTING, DisplayState.NORMAL_STOP, () -> {
			getCore().getCameraManager().setSaveImage(false);
			checkToFillImages();
			System.out.println("testing to normal stop");
			Platform.runLater(() -> this.stopTrial(false));
		});

		displaySM.setTransitionFunction(DisplayState.TESTING, DisplayState.RESTING, () -> {
			getCore().getProgramInfoWriter().completeExperiment(displaySM.getCurExperimentIndex(), displaySM.getCurExperiment().getName());

			patternDrawer.stopAndBlackOutScreen();
			for (ChildOMRController child : getCore().getChildOMRControllers())
				child.getPatternDrawer().stopAndBlackOutScreen();

			getCore().getCameraManager().setSaveImage(false);
			checkToFillImages();
		});

		displaySM.setTransitionFunction(DisplayState.RESTING, DisplayState.TESTING, () -> {
			patternDrawer.getPatternData().setLightBrightness(patternDrawer.getPatternData().getLightBrightness() - displaySM.getCurExperiment().getDimAmount());
			patternDrawer.start();
			for (ChildOMRController child : getCore().getChildOMRControllers()) {
				child.getPatternDrawer().getPatternData().setLightBrightness(patternDrawer.getPatternData().getLightBrightness() - displaySM.getCurExperiment().getDimAmount());
				child.getPatternDrawer().start();
			}

			getCore().getCameraManager().setSaveImage(true);
			getCameraImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrial()).mkdir();
			getCore().getCameraManager().resetImageIndex();
			getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrial()).getPath());
		});

		displaySM.setTransitionFunction(DisplayState.RESTING, DisplayState.IN_BETWEEN_EXPERIMENTS, () -> {}); // nothing happens

		displaySM.setTransitionFunction(DisplayState.IN_BETWEEN_EXPERIMENTS, DisplayState.TESTING, () -> {
			patternDrawer.setPatternData(displaySM.getCurExperiment().getInitialPattern());
			patternDrawer.start();
			for (ChildOMRController child : getCore().getChildOMRControllers()) {
				child.getPatternDrawer().setPatternData(displaySM.getCurExperiment().getInitialPattern());
				child.getPatternDrawer().start();
			}
			getCore().getCameraManager().resetImageIndex();
			getCore().getCameraManager().setImageIndexCap(getCore().getProgramInfoWriter().getExpectedImages(displaySM.getCurExperimentIndex(), displaySM.getCurExperiment().getName()) - 1);
			getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), 0).getPath());
		});
	}
	private void checkToFillImages() {
		lastActualImageIndex = getCore().getCameraManager().getImageIndex() - 1;
		if (lastActualImageIndex < getCore().getCameraManager().getImageIndexCap()) {
			writeTrialCameraInfoFile(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrial());
			getCore().getCameraManager().fillImagesToCap();
		}
	}

	public void stopTrial(boolean earlyStop) {
		System.out.println("stopping trial");
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

		displaySM.stopRunning(!earlyStop);
		visualizedImageReader.stopRunning();
		if (cameraManagerExecutorHandler != null && !cameraManagerExecutorHandler.isCancelled())
			cameraManagerExecutorHandler.cancel(true);
	}

	@FXML
	private Canvas canvas;

	public void startTrials(MonitorFormat monitorFormat, ArrayList<Experiment> experiments, int restTime) {
		long updateIntervalNanos = 1_000_000_000L / getCore().getProgramInfoWriter().getFPS();

		patternDrawer = new PatternDrawer(monitorFormat, experiments.getFirst().getInitialPattern(), canvas, SimulatedSurface.CIRCULAR);
		patternDrawer.start();
		for (ChildOMRController child : getCore().getChildOMRControllers())
			child.getPatternDrawer().start();

		displaySM.runExperiments(experiments, updateIntervalNanos, restTime);
		getCore().getProgramInfoWriter().activateExperiments(experiments);

		CameraManager cm = getCore().getCameraManager();
		cm.startRecording();
		cm.setSaveImage(true);
		cm.setImageIndexCap(getCore().getProgramInfoWriter().getExpectedImages(0, experiments.getFirst().getName()) - 1);

		// create proper file structure
		for (int i=0; i<experiments.size(); i++) {
			Experiment experiment = experiments.get(i);
			boolean ec = getCameraImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tc = getCameraImageTrialFolder(experiment.getName(), i, 0).mkdir();
			boolean ev = getVisualizedImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tv = getVisualizedImageTrialFolder(experiment.getName(), i, 0).mkdir();
		}

		// specify where the camera should save the raw images
		getCore().getCameraManager().setImageSavePath(getCameraImageTrialFolder(experiments.getFirst().getName(), 0, 0).getPath());

		// manage reading visualized images from python on separate thread so python lag does not block java program
		visualizedImageReader.start(updateIntervalNanos);

		// manage camera on separate thread for custom FPS
		cameraManagerExecutorHandler = cameraManagerExecutor.scheduleAtFixedRate(() -> {
			try {
				cm.update();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, updateIntervalNanos, TimeUnit.NANOSECONDS);
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

	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}
	public DisplayStateManager getDisplaySM() {
		return displaySM;
	}

	public void shutDownExecutors() {
		displaySM.shutDownExecutor();
		cameraManagerExecutor.shutdown();
		visualizedImageReader.shutDownExecutor();
	}
}
