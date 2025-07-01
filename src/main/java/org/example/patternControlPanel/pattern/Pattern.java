package org.example.patternControlPanel.pattern;


import org.json.JSONObject;
import org.example.patternControlPanel.controlPanel.PatternDirection;


public class Pattern {
	
	private String name;
	private PatternDirection direction;
	// speed in rotations per minute, brightness from 0 to 1, bandWidth in centimeters
	private double speed, brightness, bandWidth;
	
	public Pattern(String name, PatternDirection direction, double speed, double brightness, double bandWidth) {
		this.name = name;
		this.direction = direction;
		this.speed = speed;
		this.brightness = brightness;
		this.bandWidth = bandWidth;
	}

	@Override
	public String toString() {
		return name;
	}
	public String toStringDetails() {
		return  name + " | " + direction + " | " + speed + " rot/min | " + brightness + " brightness | " + bandWidth + " cm bands";
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public PatternDirection getDirection() {
		return direction;
	}
	public void setDirection(PatternDirection direction) {
		this.direction = direction;
	}
	
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		if (speed < 0)
			throw new IllegalArgumentException("speed has to be positive");
		this.speed = speed;
	}
	
	public double getBrightness() {
		return brightness;
	}
	public void setBrightness(double brightness) {
		if (brightness < 0 || brightness > 1)
			throw new IllegalArgumentException("brightness has to be between 0 and 1");
		this.brightness = brightness;
	}
	
	public double getBandWidth() {
		return bandWidth;
	}
	public void setBandWidth(double bandWidth) {
		if (brightness < 0)
			throw new IllegalArgumentException("bandwidth has to be positive");
		this.bandWidth = bandWidth;
	}
	
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("isClockwise", direction == PatternDirection.CLOCKWISE);
		obj.put("speed", speed);
		obj.put("brightness", brightness);
		obj.put("bandWidth", bandWidth);
		return obj;
	}
	
	// convert from rotations/minute to pixels/second for patternControlPanel.pattern band speed
//	public double getSpeedPixelsPerSecond() {
//		double rotationsPerSec = speed / 60;
//		double percentScreenPerSec = rotationsPerSec / 4; // percent of screen to cover in 1 second - there are 4 screens
//		double pixelsPerScreenPerSec = percentScreenPerSec * getScreenPixelWidth();
//		return pixelsPerScreenPerSec;
//	}
}
