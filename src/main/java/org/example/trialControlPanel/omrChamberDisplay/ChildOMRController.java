package org.example.trialControlPanel.omrChamberDisplay;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.parentClasses.CustomController;
import org.example.trialControlPanel.pattern.PatternDrawer;
import org.example.trialControlPanel.trialConfig.Experiment;

import java.util.ArrayList;

public class ChildOMRController extends CustomController {

    private PatternDrawer patternDrawer;

    @FXML
    private Canvas canvas;

    public void initPatternDrawer(MonitorFormat monitorFormat, ArrayList<Experiment> trials) {
        patternDrawer = new PatternDrawer(monitorFormat, trials.getFirst().getInitialPattern(), canvas, PatternDrawer.SimulatedSurface.CIRCULAR);
    }
    public void resizeCanvas(int width, int height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
    }
    public PatternDrawer getPatternDrawer() {
        return patternDrawer;
    }
}
