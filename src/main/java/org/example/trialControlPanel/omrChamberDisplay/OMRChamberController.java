package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import org.example.cameraCode.CameraManager;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OMRChamberController extends CustomController {

	private static final long MAX_SETUP_TIME = 10;
	private static final int SETUP_CONSISTENCY_FRAMES = 10;
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
		visualizedImageReader.connectInputStream();
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
			getCore().getCameraManager().resetImageIndex();
			getCameraImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrial()).mkdir();
			getVisualizedImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrial()).mkdir();
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
			getCore().getCameraManager().setSaveImage(true);
			getCore().getCameraManager().resetImageIndex();
			getCore().getCameraManager().setImageIndexCap(getCore().getProgramInfoWriter().getExpectedImages(displaySM.getCurExperimentIndex(), displaySM.getCurExperiment().getName()) - 1);
			getCameraImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), 0).mkdir();
			getVisualizedImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrial()).mkdir();
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

	public void setupAndStartExperiments(MonitorFormat monitorFormat, ArrayList<Experiment> experiments, int restTime) {

		// create starting file structure
		File setupImagesFolder = Paths.get(getCore().getStartMenuController().getCameraOutputPath(), "setup").toFile();
		setupImagesFolder.mkdir();
		for (int i=0; i<experiments.size(); i++) {
			Experiment experiment = experiments.get(i);
			boolean ec = getCameraImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tc = getCameraImageTrialFolder(experiment.getName(), i, 0).mkdir();
			boolean ev = getVisualizedImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tv = getVisualizedImageTrialFolder(experiment.getName(), i, 0).mkdir();
		}

		// setup camera manager
		CameraManager cm = getCore().getCameraManager();
		cm.startRecording();
		cm.resetImageIndex();
		cm.setSaveImage(true);
		cm.setImageIndexCap(-1); // no image cap
		cm.setImageSavePath(setupImagesFolder.getPath());

		// show blank display
		patternDrawer = new PatternDrawer(monitorFormat, experiments.getFirst().getInitialPattern(), canvas, SimulatedSurface.CIRCULAR);
		patternDrawer.stopAndBlackOutScreen();
		for (ChildOMRController child : getCore().getChildOMRControllers())
			child.getPatternDrawer().stopAndBlackOutScreen();

		// calculate desired update interval
		long updateIntervalNanos = 1_000_000_000L / getCore().getProgramInfoWriter().getFPS();

		// setup period - finishes once camera reaches stable FPS or when 5 seconds have past
		new Thread(() -> {
			long startTime = System.nanoTime();
			long lastUpdateTimeNanos = 0;
			ArrayList<Long> pastDts = new ArrayList<>();
			while (true) {
				if (System.nanoTime() - startTime > MAX_SETUP_TIME * 1_000_000_000L)
					break;
				pastDts.addFirst(System.nanoTime() - lastUpdateTimeNanos);
				lastUpdateTimeNanos = System.nanoTime();
				if (pastDts.size() > SETUP_CONSISTENCY_FRAMES) {
					boolean allGood = true;
					for (int i = 0; i < SETUP_CONSISTENCY_FRAMES; i++) {
						if (pastDts.get(i) > updateIntervalNanos) {
							allGood = false;
							break;
						}
					}
					if (allGood)
						break;
				}
				cm.update();
			}
			startExperiments(experiments, updateIntervalNanos, restTime);
		}).start();
	}
	private void startExperiments(ArrayList<Experiment> experiments, long updateIntervalNanos, int restTime) {
		// start experiments
		patternDrawer.start();
		for (ChildOMRController child : getCore().getChildOMRControllers())
			child.getPatternDrawer().start();

		displaySM.runExperiments(experiments, updateIntervalNanos, restTime);
		getCore().getProgramInfoWriter().activateExperiments(experiments);

		// manage camera on separate thread for custom FPS
		CameraManager cm = getCore().getCameraManager();
		cm.resetImageIndex();
		cm.setImageIndexCap(getCore().getProgramInfoWriter().getExpectedImages(0, experiments.getFirst().getName()) - 1);
		cm.setImageSavePath((getCameraImageTrialFolder(experiments.getFirst().getName(), 0, 0).getPath()));
		cameraManagerExecutorHandler = cameraManagerExecutor.scheduleAtFixedRate(cm::update, 0, updateIntervalNanos, TimeUnit.NANOSECONDS);

		// manage reading visualized images from python on separate thread so python lag does not block java program
		visualizedImageReader.start(updateIntervalNanos);
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
