package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import org.example.trialControlPanel.sceneManager.CustomController;
import org.example.trialControlPanel.pattern.MonitorFormat;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.trialControlPanel.trialConfig.TrialConfig;

import java.util.Objects;

public class OMRChamberController extends CustomController {
	
	private MonitorFormat monitorFormat;
	private TrialConfig trialConfig;
	private PatternDrawer patternDrawer;

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

	public void setMonitorFormat(MonitorFormat monitorFormat) {
		this.monitorFormat = monitorFormat;
	}
	public void setTrialConfig(TrialConfig trialConfig) {
		this.trialConfig = trialConfig;
	}

	public void initPatternDrawer() {
		patternDrawer = new PatternDrawer(monitorFormat, trialConfig.getInitialPattern(), canvas, SimulatedSurface.CIRCULAR);
	}

	public void startTrials() {
		patternDrawer.start();
	}
	public void showBlank() {
		patternDrawer.showBlank();
	}
	
	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}
	
}
