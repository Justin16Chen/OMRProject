package org.example.trialControlPanel.omrChamberDisplay;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import org.example.cameraCode.CameraManager;
import org.example.cameraCode.VisualizedImageReader;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;

public class OMRChamberController extends CustomController {
	public PatternDrawer patternDrawer;
	private DisplayStateManager displaySM;
	public VisualizedImageReader visualizedImageReader;

	@Override
	public void setup() {
		visualizedImageReader = new VisualizedImageReader(getCore());
		displaySM = new DisplayStateManager(getCore(), visualizedImageReader);
		getCore().getCameraManager().visualizedImageReader = visualizedImageReader;

		declareDisplaySMStateLogic();
	}

	public void connectVisualizedImageInputSocket() {
		visualizedImageReader.connectInputStream();
	}
	private void declareDisplaySMStateLogic() {
		// this happens after stopTrial is called, so it runs in the background after the OMR chamber windows have been closed
		displaySM.setUpdateFunction(DisplayState.NORMAL_STOP, () -> {
			CameraManager cm = getCore().getCameraManager();
//			System.out.println("send state: " + cm.getSendState() + " | save state: " + cm.getSaveState() + " | send idx: " + cm.getSendIndex() + "/" + (cm.getMaxImageIndex() + 1));
			if (cm.finishedSendingImagesToSSD() && cm.getSaveState() == CameraManager.SaveState.WAITING && visualizedImageReader.getState() == VisualizedImageReader.State.WAITING_TO_SAVE) {
				cm.saveImageData(getCameraImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrialIndex()).toString());
				visualizedImageReader.saveAndClearStoredImages(getVisualizedImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrialIndex()).toString());
				System.out.println("finished saving everything after experiments ended");
			}
		});
		// early stop is handled by javaFX event listeners
		displaySM.setTransitionFunction(DisplayState.TESTING, DisplayState.RESTING, () -> {
			patternDrawer.stopAndBlackOutScreen();
			for (ChildOMRController child : getCore().getChildOMRControllers())
				child.getPatternDrawer().stopAndBlackOutScreen();

			getCore().getCameraManager().getImageGrabber().stopGrabbing();
		});
		displaySM.setUpdateFunction(DisplayState.RESTING, () -> {
			CameraManager cm = getCore().getCameraManager();
			if (cm.finishedSendingImagesToSSD() && cm.getSaveState() == CameraManager.SaveState.WAITING && visualizedImageReader.getState() == VisualizedImageReader.State.WAITING_TO_SAVE) {
				cm.saveImageData(getCameraImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrialIndex()).toString());
				visualizedImageReader.saveAndClearStoredImages(getVisualizedImageTrialFolder(displaySM.getCurExperiment().getName(), displaySM.getCurExperimentIndex(), displaySM.getCurTrialIndex()).toString());
			}
		});

		displaySM.setTransitionFunction(DisplayState.RESTING, DisplayState.TESTING, () -> {
			patternDrawer.getPatternData().setLightBrightness(patternDrawer.getPatternData().getLightBrightness() - displaySM.getCurExperiment().getDimAmount());
			patternDrawer.start();
			for (ChildOMRController child : getCore().getChildOMRControllers()) {
				child.getPatternDrawer().getPatternData().setLightBrightness(patternDrawer.getPatternData().getLightBrightness() - displaySM.getCurExperiment().getDimAmount());
				child.getPatternDrawer().start();
			}

			long updateIntervalNanos = (long) (1e9 / getCore().fps);
			getCore().getCameraManager().clearOldImageData();
			getCore().getCameraManager().startReadingImagesFromCamera(updateIntervalNanos);
			getCore().getCameraManager().startSendingImagesToSSD();
			visualizedImageReader.startReadingVisualizedImages();

			String experimentName = displaySM.getCurExperiment().getName();
			int experimentIndex = displaySM.getCurExperimentIndex();
			int trialNum = displaySM.getCurTrialIndex();

			getCameraImageTrialFolder(experimentName, experimentIndex, trialNum).mkdir();
			getVisualizedImageTrialFolder(experimentName, experimentIndex, trialNum).mkdir();
		});

		displaySM.setTransitionFunction(DisplayState.IN_BETWEEN_EXPERIMENTS, DisplayState.TESTING, () -> {
			// prepare for next experiment
			patternDrawer.setPatternData(displaySM.getCurExperiment().getInitialPattern());
			patternDrawer.start();
			for (ChildOMRController child : getCore().getChildOMRControllers()) {
				child.getPatternDrawer().setPatternData(displaySM.getCurExperiment().getInitialPattern());
				child.getPatternDrawer().start();
			}
			long updateIntervalNanos = (long) (1e9 / getCore().fps);
			getCore().getCameraManager().clearOldImageData();
			getCore().getCameraManager().startReadingImagesFromCamera(updateIntervalNanos);
			getCore().getCameraManager().startSendingImagesToSSD();
			visualizedImageReader.startReadingVisualizedImages();

			;
			getCore().getCameraManager().setMaxImageIndex(displaySM.getCurExperiment().getTestTime() * getCore().fps - 1);

			String experimentName = displaySM.getCurExperiment().getName();
			int experimentIndex = displaySM.getCurExperimentIndex();
			int trialNum = displaySM.getCurTrialIndex();

			getCameraImageTrialFolder(experimentName, experimentIndex, trialNum).mkdir();
			getVisualizedImageTrialFolder(experimentName, experimentIndex, trialNum).mkdir();
		});
	}

	public void stopShowingExperiments(boolean earlyStop) {
		patternDrawer.stop();
		Platform.runLater(getStage()::close);
		for (ChildOMRController child : getCore().getChildOMRControllers()) {
			Platform.runLater(() -> {
				child.getStage().close();
				child.getPatternDrawer().stop();
			});
		}

		getCore().getCameraManager().getImageGrabber().stopGrabbing();

		// this stuff could still be running in the background for normal stops
		if (earlyStop) {
			getCore().getCameraManager().stopSendingImages();
			visualizedImageReader.stopRunning();
            displaySM.stopUpdating();
		}

		// stuff that happens after chamber display ends
		getCore().getStartMenuController().updateButtonsEnabled();
		getCore().getTrialMetadataStage().setScene(getCore().getLoadingResultsScene());
		getCore().getResultsController().earlyStop = earlyStop;
		new Thread(() -> {
			boolean canMoveOn = getCore().getCameraManager().getSendState() == CameraManager.SendState.READY
					&& getCore().getCameraManager().getSaveState() == CameraManager.SaveState.READY
					&& visualizedImageReader.getState() == VisualizedImageReader.State.WAITING_TO_RECEIVE;
			while (!canMoveOn) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
				canMoveOn = getCore().getCameraManager().getSendState() == CameraManager.SendState.READY
						&& getCore().getCameraManager().getSaveState() == CameraManager.SaveState.READY
						&& visualizedImageReader.getState() == VisualizedImageReader.State.WAITING_TO_RECEIVE;
            }
			Platform.runLater(() -> {
				getCore().getTrialMetadataStage().setScene(getCore().getResultsScene());
				getCore().getResultsController().setup();
				System.out.println("SETTING RESULTS SCENE TO NORMAL");
			});
		}, "wait for finished results to open results scene").start();
	}

	@FXML
	private Canvas canvas;

	public void setupAndStartExperiments(MonitorFormat monitorFormat, ArrayList<Experiment> experiments, int restTime) {

		// create starting file structure
		for (int i=0; i<experiments.size(); i++) {
			Experiment experiment = experiments.get(i);
			boolean ec = getCameraImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tc = getCameraImageTrialFolder(experiment.getName(), i, 0).mkdir();
			boolean ev = getVisualizedImageExperimentFolder(experiment.getName(), i).mkdir();
			boolean tv = getVisualizedImageTrialFolder(experiment.getName(), i, 0).mkdir();
		}

		// setup camera manager
		CameraManager cm = getCore().getCameraManager();
		cm.clearOldImageData();
		cm.setMaxImageIndex(-1); // no image cap

		// show blank display
		patternDrawer = new PatternDrawer(monitorFormat, experiments.getFirst().getInitialPattern(), canvas, SimulatedSurface.CIRCULAR);
		patternDrawer.stopAndBlackOutScreen();
		for (ChildOMRController child : getCore().getChildOMRControllers())
			child.getPatternDrawer().stopAndBlackOutScreen();

		// first wait for camera grabber to grab at a steady FPS
		getCore().getCameraManager().getImageGrabber().startGrabbing();
		Thread waitForStableFPSThread = new Thread(() -> {
			long unstableFPSSleepTimeMs = 1000 / 30;
			while (!getCore().getCameraManager().getImageGrabber().reachedStableFPS()) {
				try {
					Thread.sleep(unstableFPSSleepTimeMs);
				} catch (InterruptedException ignored) {
				}
			}

			// once stable FPS is reached, start experiments
			long updateIntervalNanos = 1_000_000_000L / getCore().fps;
			Platform.runLater(() -> startExperiments(experiments, updateIntervalNanos, restTime) );
		});
		waitForStableFPSThread.start();
	}
	private void startExperiments(ArrayList<Experiment> experiments, long updateIntervalNanos, int restTime) {
		patternDrawer.start();
		for (ChildOMRController child : getCore().getChildOMRControllers())
			child.getPatternDrawer().start();

		displaySM.runExperiments(experiments, updateIntervalNanos, restTime);

		// setup camera
		CameraManager cm = getCore().getCameraManager();
		cm.clearOldImageData();
		cm.setMaxImageIndex(displaySM.getCurExperiment().getTestTime() * getCore().fps - 1);

		// start grabbing images on a separate thread and read and store the images at a fixed interval
		getCore().getCameraManager().startReadingImagesFromCamera(updateIntervalNanos);
		// start thread to send images to SSD for visualizing
		cm.startSendingImagesToSSD();
		// start thread to receive visualized images from SSD
		visualizedImageReader.startReadingVisualizedImages();
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

	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}
	public DisplayStateManager getDisplaySM() {
		return displaySM;
	}
}
