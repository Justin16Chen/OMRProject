package org.example.patternControlPanel.display;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import org.example.patternControlPanel.pattern.MonitorFormat;
import org.example.patternControlPanel.pattern.Pattern;
import org.example.patternControlPanel.pattern.PatternDrawer;
import org.example.patternControlPanel.pattern.PatternDrawer.SimulatedSurface;

public class DisplayController {
	
	private MonitorFormat monitorFormat;
	private Pattern pattern;
	private PatternDrawer patternDrawer;
	
	@FXML
	private Canvas canvas;
	
	public void setMonitorFormat(MonitorFormat monitorFormat) {
		this.monitorFormat = monitorFormat;
	}
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}
	
	public void start() {
		patternDrawer = new PatternDrawer(monitorFormat, pattern, canvas, SimulatedSurface.CIRCULAR);
		patternDrawer.start();
	}
	
	public void resizeCanvas(int width, int height) {
		canvas.setWidth(width);
		canvas.setHeight(height);
	}
	
}
