package org.example.trialControlPanel.monitorInfo;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.List;

public class MonitorFormat {
	
	public static int getNumScreens() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return ge.getScreenDevices().length;
	}
	public static boolean hasScreen(int screenNumber) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return screenNumber <= ge.getScreenDevices().length;
	}

	private static final DecimalFormat df = new DecimalFormat("0.00");

	// width and height in cm does not have 3 sig figs, 3 decimal places were chosen for visuals
	private final int monitorNumber; // 1 is first monitor - this is not an index - this is a number
	private final int widthPixels, heightPixels; // width and height of monitor in pixels
	private final double widthCM, heightCM; // width and height of monitor in centimeters
	private final double scale; // scale of screen ex: 1, 1.25, 1.5, 1.75, 2
	
	// there MUST be a JavaFX application running for this to work
	public MonitorFormat(int monitorNumber) {
		this.monitorNumber = monitorNumber;

        // Get the local graphics environment
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        
        // Get all screen devices (monitors)
        GraphicsDevice[] screens = ge.getScreenDevices();
        int monitorIndex = monitorNumber - 1;
       
        if (monitorIndex >= screens.length)
        	throw new IllegalArgumentException("Monitor with number of " + monitorNumber +" is greater than the number of monitors (" + screens.length + ")");
        
        // get patternControlPanel.display mode for monitor
        DisplayMode displayMode = screens[monitorIndex].getDisplayMode();
        widthPixels = displayMode.getWidth();
        heightPixels = displayMode.getHeight();

        // Get DPI for the selected monitor
        GraphicsConfiguration gc = screens[monitorIndex].getDefaultConfiguration();
        AffineTransform transform = gc.getDefaultTransform();
        scale = transform.getScaleX(); // assumes that scaleX and scaleY are the same (in settings they are)

        // Get a list of all monitors using JavaFX for accurate DPI
        List<Screen> screens2 = Screen.getScreens();
        double dpi = screens2.get(monitorIndex).getDpi() * scale;

        // Convert pixels to centimeters
        widthCM = (widthPixels / dpi) * 2.54;
        heightCM = (heightPixels / dpi) * 2.54;
			
    }
	
	public int getMonitorNumber() {
		return monitorNumber;
	}
	
	// scales the pixel res so client doesn't have to worry about scaling
	public int getWidthPixels() {
		return (int) (widthPixels / scale);
	}
	public int getHeightPixels() {
		return (int) (heightPixels / scale);
	}
	// gets the scale of the monitor this is for
	public double getVirtualScale() {
		return scale;
	}
	
	public double getWidthCM() {
		return widthCM;
	}
	public double getHeightCM() {
		return heightCM;
	}
	public double getPixelsPerCenti() {
		return widthPixels / widthCM;
	}
	public double getVirtualPixelsPerCenti() {
		return widthPixels / widthCM / scale;
	}
	
	public Rectangle2D getBounds() {
        return Screen.getScreens().get(monitorNumber - 1).getBounds();
	}
	
	@Override
	public String toString() {
		return monitorNumber + ": " + widthPixels + "x" + heightPixels + " pixels, " + df.format(widthCM) + "x" + df.format(heightCM) + " cm";
	}
	public String getSizeSpecs() {
		return df.format(widthCM) + "x" + df.format(heightCM) + "cm";
	}
	public String getResolutionSpecs() {
		return widthPixels + "x" + heightPixels + "px";
	}
}
