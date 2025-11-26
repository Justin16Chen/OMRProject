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
		// early stop is handled by javaFX event listeners
		displaySM.setTransitionFunction(DisplayState.TESTING, DisplayState.RESTING, () -> {
			patternDrawer.stopAndBlackOutScreen();
			for (ChildOMRController child : getCore().getChildOMRControllers())
				child.getPatternDrawer().stopAndBlackOutScreen();

			getCore().getCameraManager().getImageGrabber().stopGrabbing();
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

			getCore().getCameraManager().setMaxImageIndex(displaySM.getCurExperiment().getTestTime() * getCore().fps - 1);
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
		getCore().getTrialMetadataStage().close();
		Platform.runLater(() -> getCore().getStartMenuController().exportingLabel.setText("Currently Exporting..."));
		new Thread(() -> {
			while (!displaySM.canSaveResults) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			Platform.runLater(() -> getCore().getStartMenuController().saveAllResults());
			Platform.runLater(() -> getCore().getStartMenuController().exportingLabel.setText("Finished!"));
			new Thread(() -> {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				Platform.runLater(() -> getCore().getStartMenuController().exportingLabel.setText(""));
			}).start();

		}, "wait for finished results to open results scene").start();
	}

	@FXML
	private Canvas canvas;

	public void setupAndStartExperiments(MonitorFormat monitorFormat, ArrayList<Experiment> experiments, int restTime) {

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

	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}
	public DisplayStateManager getDisplaySM() {
		return displaySM;
	}
}
