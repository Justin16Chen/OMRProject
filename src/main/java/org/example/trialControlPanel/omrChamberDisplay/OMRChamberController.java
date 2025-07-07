package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.TrialConfig;

import java.util.Objects;

public class OMRChamberController extends CustomController {

	enum State {
		TESTING, RESTING
	}
	private MonitorFormat monitorFormat;
	private TrialConfig trial;
	private PatternDrawer patternDrawer;
	private boolean trialRunning;
	private double totalSecondsRunning, currentCycleSecondsRunning; // 1 cycle = 1 test and 1 rest
	private State state;

	@Override
	public void setup() {
		getSceneManager().getOMRChamberScene().setOnKeyPressed(e -> {
            if (Objects.requireNonNull(e.getCode()) == KeyCode.ESCAPE) {
				getStage().close();
				patternDrawer.stop();
			}
		});
	}

	@FXML
	private Canvas canvas;

	public void initPatternDrawer(MonitorFormat monitorFormat, TrialConfig trial) {
		this.monitorFormat = monitorFormat;
		this.trial = trial;

		patternDrawer = new PatternDrawer(monitorFormat, trial.getInitialPattern(), canvas, SimulatedSurface.CIRCULAR);
	}

	public void startTrials() {
		trialRunning = true;
		state = State.TESTING;
		patternDrawer.start();

		// manage trials on separate thread
		new Thread(() -> {
			double startTimeMs = System.currentTimeMillis();
			double lastCycleFinishTime = startTimeMs;

			while (trialRunning) {
				totalSecondsRunning = (System.currentTimeMillis() - startTimeMs) / 1000.;
				currentCycleSecondsRunning = (System.currentTimeMillis() - lastCycleFinishTime) / 1000.;

				// webcam streaming here prob

				/*
				
				*/

				if (state == State.TESTING) {

				}
				else {

				}

				if (currentCycleSecondsRunning > trial.getTotalTime()) {
					trialRunning = false;
					patternDrawer.stop();
					patternDrawer.showBlank();
				}
			}
		}).start();
	}
	
	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}

	public int getTestRunTime() {
		return (int) currentCycleSecondsRunning;
	}
	public int getRestRunTime() {
		return 0;
	}
}
