package org.example.patternControlPanel.pattern;


import org.json.JSONObject;


public class Pattern {
	
	private PatternDirection direction;
	// speed in rotations per minute, brightness from 0 to 1, bandWidth in centimeters
	private double speed, lightBrightness, darkBrightness, bandWidth;
	
	public Pattern(PatternDirection direction, double speed, double lightBrightness, double darkBrightness, double bandWidth) {
		this.direction = direction;
		this.speed = speed;
		this.lightBrightness = lightBrightness;
		this.darkBrightness = darkBrightness;
		this.bandWidth = bandWidth;
	}

	@Override
	public String toString() {
		return direction + " | " + speed + " rot/min | " + lightBrightness + ", " + darkBrightness + " brightness | " + bandWidth + " cm bands";
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
	
	public double getLightBrightness() {
		return lightBrightness;
	}
	public void setLightBrightness(double lightBrightness) {
		if (lightBrightness < 0 || lightBrightness > 255)
			throw new IllegalArgumentException("lightBrightness has to be between 0 and 255");
		this.lightBrightness = lightBrightness;
	}
	public double getDarkBrightness() { return darkBrightness; }
	public void setDarkBrightness(double darkBrightness) {
		if (darkBrightness < 0 || darkBrightness > 255)
			throw new IllegalArgumentException("darkBrightness has to be between 0 and 255");
		this.darkBrightness = darkBrightness;
	}
	
	public double getBandWidth() {
		return bandWidth;
	}
	public void setBandWidth(double bandWidth) {
		if (bandWidth < 0)
			throw new IllegalArgumentException("bandwidth has to be positive");
		this.bandWidth = bandWidth;
	}
}
