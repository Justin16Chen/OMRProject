package org.example.patternControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import org.example.patternControlPanel.sceneManager.CustomController;
import org.example.patternControlPanel.pattern.MonitorFormat;
import org.example.patternControlPanel.pattern.PatternDrawer;
import org.example.patternControlPanel.pattern.PatternDrawer.SimulatedSurface;
import org.example.patternControlPanel.trialConfig.TrialConfig;

import java.util.Objects;

public class OMRChamberController extends CustomController {
	
	private MonitorFormat monitorFormat;
	private TrialConfig trialConfig;
	private PatternDrawer patternDrawer;

	@Override
	public void setup() {
		getSceneManager().getOMRChamberScene().setOnKeyPressed(e -> {
            if (Objects.requireNonNull(e.getCode()) == KeyCode.ESCAPE)
                getStage().close();
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
