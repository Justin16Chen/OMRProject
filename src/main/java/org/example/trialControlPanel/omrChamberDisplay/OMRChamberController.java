package org.example.trialControlPanel.omrChamberDisplay;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
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

	public enum State {
		TESTING, RESTING
	}
	private MonitorFormat monitorFormat;
	private TrialConfig trial;
	private PatternDrawer patternDrawer;
	private boolean trialRunning;
	private double totalSecondsRunning, currentCycleSecondsRunning; // 1 cycle = 1 test and 1 rest
	private State state;
	private int cycle;

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
		cycle = 1;


		AnimationTimer timer = new AnimationTimer() {
			private long startTime = -1;
			private double lastCycleFinishTimeSeconds;
			@Override
			public void handle(long now) {
				if (startTime == -1) {
					startTime = now;
					lastCycleFinishTimeSeconds = now / 1_000_000_000.;
				}
				long elapsedNanos = now - startTime;
				totalSecondsRunning = elapsedNanos / 1_000_000_000.;
				currentCycleSecondsRunning = totalSecondsRunning - lastCycleFinishTimeSeconds;

				if (state == State.TESTING) {
					updateTesting();

					if (currentCycleSecondsRunning > trial.getTestTime())
						state = State.RESTING;
				}
				else {
					updateResting();

					if (currentCycleSecondsRunning - trial.getTestTime() > trial.getRestTime()) {
						lastCycleFinishTimeSeconds = now / 1_000_000_000.;
						state = State.TESTING;
						cycle++;
					}
				}

				getSceneManager().getRunTrialController().updateUI();

				if (cycle > trial.getMaxTests()) {
					trialRunning = false;
					patternDrawer.stop();
					patternDrawer.showBlank();
					stop();
				}
			}
		};
		timer.start();
	}
	
	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}

	public int getTestRunTime() {
		return state == State.TESTING ? (int) currentCycleSecondsRunning : 0;
	}
	public int getRestRunTime() {
		return state == State.RESTING ? (int) (currentCycleSecondsRunning - trial.getTestTime()) : 0;
	}
	public int getTotalRunTime() {
		return (int) totalSecondsRunning;
	}
	public int getCycle() {
		return cycle;
	}
	public State getState() {
		return state;
	}

	private void updateTesting() {
		if (!patternDrawer.isPlaying())
			patternDrawer.start();
	}
	private void updateResting() {
		if (patternDrawer.isPlaying()) {
			patternDrawer.stop();
			patternDrawer.showBlank();
		}
	}
}
