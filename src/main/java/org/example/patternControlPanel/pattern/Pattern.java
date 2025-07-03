package org.example.patternControlPanel.pattern;


import org.json.JSONObject;


public class Pattern {
	
	private String name;
	private PatternDirection direction;
	// speed in rotations per minute, brightness from 0 to 1, bandWidth in centimeters
	private double speed, lightBrightness, darkBrightness, bandWidth;
	
	public Pattern(String name, PatternDirection direction, double speed, double lightBrightness, double darkBrightness, double bandWidth) {
		this.name = name;
		this.direction = direction;
		this.speed = speed;
		this.lightBrightness = lightBrightness;
		this.darkBrightness = darkBrightness;
		this.bandWidth = bandWidth;
	}

	@Override
	public String toString() {
		return name;
	}
	public String toStringDetails() {
		return  name + " | " + direction + " | " + speed + " rot/min | " + lightBrightness + ", " + darkBrightness + " brightness | " + bandWidth + " cm bands";
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
	
	public double getLightBrightness() {
		return lightBrightness;
	}
	public void setLightBrightness(double lightBrightness) {
		if (lightBrightness < 0 || lightBrightness > 255)
			throw new IllegalArgumentException("lightBrightness has to be between 0 and 255");
		this.lightBrightness = lightBrightness;
	}
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
	
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
		obj.put("name", name);
		obj.put("isClockwise", direction == PatternDirection.CLOCKWISE);
		obj.put("speed", speed);
		obj.put("lightBrightness", lightBrightness);
		obj.put("darkBrightness", darkBrightness);
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
