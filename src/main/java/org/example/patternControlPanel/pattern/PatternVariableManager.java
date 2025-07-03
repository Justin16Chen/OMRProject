package org.example.patternControlPanel.pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class PatternVariableManager {
	// monitor patternControlPanel.pattern parameters: 30, 2560, 2
	public static final String FILE_PATH = "PatternVariables.json";
	public static final Pattern DEFAULT_PATTERN_DATA = new Pattern("default", PatternDirection.CLOCKWISE, 10, 0.5, 0, 2);
	
	// get singular patternControlPanel.pattern from file
	public static Pattern getPatternDataFromFile(String name) {
		JSONArray jsonPatterns = getJSONObjectFromFile().getJSONArray("patterns");

		for (int i=0; i<jsonPatterns.length(); i++) {
			JSONObject jsonPattern = jsonPatterns.getJSONObject(i);
			if (jsonPattern.getString("name").equals(name))
				return getPatternDataFromJSONObject(jsonPattern);
		}
		if (name.equals(DEFAULT_PATTERN_DATA.getName()))
			return DEFAULT_PATTERN_DATA;
		throw new IllegalArgumentException(name + " is not recognized as a patternControlPanel.pattern name");
	}
	
	// get all patterns from file
	public static ArrayList<Pattern> getAllPatternsFromFile() {
		ArrayList<Pattern> patterns = new ArrayList<>();
		JSONArray jsonPatterns = getJSONObjectFromFile().getJSONArray("patterns");
			
		for (int i=0; i<jsonPatterns.length(); i++) {
			JSONObject jsonPattern = jsonPatterns.getJSONObject(i);
			Pattern pattern = getPatternDataFromJSONObject(jsonPattern); // convert JSON object to patternControlPanel.pattern data
						
			patterns.add(pattern);
		}
		return patterns;
	}
	
	// converts a JSON object to a patternControlPanel.pattern data object
	private static Pattern getPatternDataFromJSONObject(JSONObject jsonPattern) {
		return new Pattern(
				jsonPattern.getString("name"),
				jsonPattern.getBoolean("isClockwise") ? PatternDirection.CLOCKWISE : PatternDirection.COUNTER_CLOCKWISE,
				jsonPattern.getDouble("speed"),
				jsonPattern.getDouble("lightBrightness"),
				jsonPattern.getDouble("darkBrightness"),
				jsonPattern.getDouble("bandWidth")
		);
	}
	
	public static boolean hasPattern(String patternName) {
		ArrayList<Pattern> patterns = getAllPatternsFromFile();
		for (Pattern pattern : patterns) {
			if (pattern.getName().equals(patternName))
				return true;
		}
		return false;
	}
	public static void updateSavedPattern(Pattern newPattern) {
		ArrayList<Pattern> patterns = getAllPatternsFromFile();
		JSONArray jsonPatterns = new JSONArray();
		for (Pattern pattern : patterns) 
			if (pattern.getName().equals(newPattern.getName())) 
				jsonPatterns.put(newPattern.toJSONObject());
			else 
				jsonPatterns.put(pattern.toJSONObject());
			
		writeToJSONFile(jsonPatterns);
	}
	public static void addPattern(Pattern pattern) {
		JSONArray jsonPatterns = getJSONObjectFromFile().getJSONArray("patterns");
		jsonPatterns.put(pattern.toJSONObject());
		
		writeToJSONFile(jsonPatterns);
	}
	
	private static void writeToJSONFile(JSONArray patterns) {
		try (FileWriter file = new FileWriter(FILE_PATH)) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("patterns", patterns);
			
            file.write(jsonObject.toString(4)); // 4 is the indentation level for pretty-printing
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private static JSONObject getJSONObjectFromFile() {
		JSONObject jsonData = null;
		try {
			File file = new File(FILE_PATH);
			
			String content = new String(Files.readAllBytes(Paths.get(file.toURI())));
			jsonData = new JSONObject(content);
		} catch(JSONException | IOException e) {
			e.printStackTrace();
		}
		return jsonData;
	}

	// removes a JSON patternControlPanel.pattern with the same name
	public static void removePattern(String patternName) {
		JSONArray jsonPatterns = getJSONObjectFromFile().getJSONArray("patterns");
		boolean foundPattern = false;
		for (int i=0; i<jsonPatterns.length(); i++) 
			if (jsonPatterns.getJSONObject(i).get("name").equals(patternName)) {
				jsonPatterns.remove(i);
				foundPattern = true;
				break;
			}
		
		// throw an error if patternName is not in the array of saved patterns
		if (!foundPattern)
			throw new IllegalArgumentException(patternName + " is not recognized as a patternControlPanel.pattern name");
		
		writeToJSONFile(jsonPatterns);
	}

	public static String getFirstPatternName() {
		// add default patternControlPanel.pattern if there are no patterns
		JSONArray patterns = getJSONObjectFromFile().getJSONArray("patterns");
		if (patterns.length() == 0) {
			addPattern(DEFAULT_PATTERN_DATA);
			return DEFAULT_PATTERN_DATA.getName(); // return default name
		}
		
		// return the name of the first patternControlPanel.pattern
		return patterns.getJSONObject(0).getString("name");
	}
}
