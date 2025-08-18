package org.example.trialControlPanel.pattern;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;
import org.example.trialControlPanel.utils.ElapsedTime;

public class PatternDrawer extends AnimationTimer {

	public enum SimulatedSurface {
		FLAT, CIRCULAR
	}
	private record BandInfo(double x, double width) {}
	
	private final MonitorFormat monitorFormat;
    private Pattern patternData;
    private final Canvas canvas;
    private final SimulatedSurface surfaceType;
    private int frameNum;
    private double lastPrintTimeMs;
    private double startTime;
    private double lastUpdateTimeMs;
    private boolean playing;

    public PatternDrawer(MonitorFormat monitorFormat, Pattern patternData, Canvas canvas, SimulatedSurface surfaceType) {
    	if (monitorFormat == null)
            throw new IllegalArgumentException("monitor format for pattern drawer cannot be null");
        if (patternData == null)
            throw new IllegalArgumentException("pattern data for pattern drawer cannot be null");
        if (canvas == null)
            throw new IllegalArgumentException("canvas for pattern drawer cannot be null");
        if (surfaceType == null)
            throw new IllegalArgumentException("surface type for pattern drawer cannot be null");

        this.monitorFormat = monitorFormat;
        this.patternData = patternData;
        this.canvas = canvas;
        this.surfaceType = surfaceType;
    }

    public Pattern getPatternData() {
        return patternData;
    }
    public void setPatternData(Pattern newPatternData) {
        this.patternData = newPatternData;
    }

    public void togglePlaying() {
        if (!playing)
            start();
        else
            stop();
    }

    @Override
    public void start() {
        super.start();
        playing = true;
        lastPrintTimeMs = System.currentTimeMillis();
        frameNum = 0;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void stop() {
        super.stop();
        playing = false;
    }
    public void stopAndBlackOutScreen() {
        stop();
        Platform.runLater(() -> {
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.BLACK);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        });
    }

    @Override
    public void handle(long now) {
        if (patternData == null || !playing) {
            return;
        }
        double dt = (System.currentTimeMillis() - lastUpdateTimeMs) / 1000.;
        lastUpdateTimeMs = System.currentTimeMillis();
        double timeRunning = (System.currentTimeMillis() - startTime) / 1000.;

//        frameNum++;
//        if (System.currentTimeMillis() - lastPrintTimeMs > 1000) {
//            lastPrintTimeMs = System.currentTimeMillis();
//            System.out.println("FPS: " + frameNum);
//            frameNum = 0;
//        }

        GraphicsContext g = getGraphicsContext();

        // Set line width and stroke color based on brightness
        double bandWidth = patternData.getBandWidth() * monitorFormat.getVirtualPixelsPerCenti();
        
        // Calculate the offset and total amount of lines to draw
        double rotationsPerSec = patternData.getSpeed() / 60;
        double percentScreenPerSec = rotationsPerSec * 4; // percent of screen to cover in 1 second - there are 4 screens

        switch (surfaceType) {
            case FLAT -> drawFlatPattern(g, timeRunning, bandWidth, percentScreenPerSec);
            case CIRCULAR -> drawCircularPattern(g, timeRunning, bandWidth, percentScreenPerSec);
        }
    }

    private GraphicsContext getGraphicsContext() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        // fill background
        Color darkColor = Color.rgb(patternData.getDarkBrightness(), patternData.getDarkBrightness(), patternData.getDarkBrightness());
        g.setFill(darkColor);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // set stripe stroke color
        Color lightColor = Color.rgb(patternData.getLightBrightness(), patternData.getLightBrightness(), patternData.getLightBrightness());
        g.setStroke(lightColor);
        return g;
    }

    private void drawFlatPattern(GraphicsContext g, double timeRunning, double bandWidth, double percentScreenPerSec) {
    	double pixelsPerSecondSpeed = percentScreenPerSec * monitorFormat.getWidthPixels();
        double offset = (timeRunning * pixelsPerSecondSpeed) % (bandWidth * 2);
        int amount = (int) (canvas.getWidth() / (bandWidth * 2)) + 2; // Buffer for edge cases

        // Draw vertical lines with spacing
        for (int i = -1; i < amount; i++) {
        	BandInfo bandInfo = new BandInfo(i * bandWidth * 2 + offset, bandWidth);
            g.setLineWidth(bandInfo.width);
            g.beginPath();
            g.moveTo(bandInfo.x, 0);
            g.lineTo(bandInfo.x, canvas.getHeight());
            g.stroke();
        }
    }
    private void drawCircularPattern(GraphicsContext g, double timeRunning, double bandWidth, double percentScreenPerSec) {
		double radius = monitorFormat.getWidthPixels() / 2.;
    	double bandAngle = Math.atan(bandWidth / 2 / radius) * 2; // the angle (in rad) that each band takes up
    	double angularSpeed = Math.PI / 2 * percentScreenPerSec; // change in angle every second
    	double angularOffset = (angularSpeed * timeRunning) % (bandAngle * 2);
    	int amount = (int) (Math.ceil(Math.PI / 2 / bandAngle));

    	for (int i=-1; i<amount+2; i+=2) {
        	double bandX = radius * Math.tan(-Math.PI / 4 + angularOffset + bandAngle * (i - 0.5));
        	double nextBandX = radius * Math.tan(-Math.PI / 4 + angularOffset + bandAngle * (i + 0.5));

        	g.setLineWidth(nextBandX - bandX);
            g.beginPath();
            g.moveTo(bandX + radius, 0);
            g.lineTo(bandX + radius, canvas.getHeight());
            g.stroke();
    	}
    }

    @Override
    public String toString() {
        return "mf: " + monitorFormat + "\npattern: " + patternData + "\n canvas: " + canvas;
    }
}
