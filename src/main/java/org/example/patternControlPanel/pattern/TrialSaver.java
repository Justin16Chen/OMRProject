package org.example.patternControlPanel.pattern;

import org.example.patternControlPanel.trialConfig.TrialConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TrialSaver {
	public static final String FILE_PATH = "TestTrials.json";
	public static final TrialConfig DEFAULT_TRIAL = new TrialConfig("defaultTrial", new Pattern(PatternDirection.CLOCKWISE, 1, 1, 0, 1), 1, 1, 1, 1);
	private static ArrayList<TrialConfig> trials;

	public static void initializeTrialSaver() {
		trials = new ArrayList<>();
		JSONArray jsonTrials = getJSONTrialsFromFile();
		for (int i=0; i<jsonTrials.length(); i++) {
			JSONObject jsonTrial = jsonTrials.getJSONObject(i);
			TrialConfig trial = getTrialDataFromJSONObject(jsonTrial); // convert JSON object to patternControlPanel.pattern data

			trials.add(trial);
		}
	}

	// gets json trials array from file
	private static JSONArray getJSONTrialsFromFile() {
		JSONObject jsonData = null;
		try {
			File file = new File(FILE_PATH);

			String content = new String(Files.readAllBytes(Paths.get(file.toURI())));
			jsonData = new JSONObject(content);
		} catch(JSONException | IOException e) {
			e.printStackTrace();
		}
		return jsonData.getJSONArray("trials");
	}

	// converts a JSON object to a patternControlPanel.pattern data object
	private static TrialConfig getTrialDataFromJSONObject(JSONObject jsonTrial) {
		JSONObject jsonPattern = jsonTrial.getJSONObject("initialPattern");

		String directionStr = jsonPattern.getString("direction").toLowerCase();
		PatternDirection direction;
		switch (directionStr) {
			case "clockwise" -> direction = PatternDirection.CLOCKWISE;
			case "counter_clockwise" -> direction = PatternDirection.COUNTER_CLOCKWISE;
			case "both" -> direction = PatternDirection.BOTH;
			default -> throw new JSONException("pattern direction of " + directionStr + " is not valid");
		}

		return new TrialConfig(
				jsonTrial.getString("name"),
				new Pattern(
						direction,
						jsonPattern.getDouble("speed"),
						jsonPattern.getDouble("lightBrightness"),
						jsonPattern.getDouble("darkBrightness"),
						jsonPattern.getDouble("bandWidth")
				),
				jsonTrial.getDouble("testTime"),
				jsonTrial.getDouble("restTime"),
				jsonTrial.getDouble("dimPercent"),
				jsonTrial.getInt("maxTests")
		);
	}

	// writes trials array to file - called anytime trials is modified
	private static void writeToJSONFile() {
		try (FileWriter file = new FileWriter(FILE_PATH)) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("trials", trials);

			file.write(jsonObject.toString(4)); // 4 is the indentation level for pretty-printing
		} catch (IOException e) {
			System.out.println("error when writing trials to json file");
			e.printStackTrace();
		}
	}

	public static TrialConfig getTrial(String name) {
		for (TrialConfig trial : trials)
			if (trial.getName().equals(name))
				return trial.deepCopy();
		throw new IllegalArgumentException(name + " is not a name of a trial");
	}
	public static boolean hasTrial(String trialName) {
		for (TrialConfig trial : trials) {
			if (trial.getName().equals(trialName))
				return true;
		}
		return false;
	}

	// either add trial or updates saved trial if there is already a trial with the same name as newTrial
	public static void addTrial(TrialConfig newTrial) {
		for (int i=0; i<trials.size(); i++) {
			if (trials.get(i).getName().equals(newTrial.getName())) {
				System.out.println("removing " + trials.get(i));
				trials.remove(i);
				break;
			}
		}
		trials.add(newTrial.deepCopy());
		System.out.println("adding " + newTrial);
		writeToJSONFile();
	}

	// removes a JSON patternControlPanel.pattern with the same name
	public static void removeTrial(String trialName) {
		for (int i=0; i<trials.size(); i++) {
			if (trials.get(i).getName().equals(trialName)) {
				trials.remove(i);
				writeToJSONFile();
				return;
			}
		}
		throw new IllegalArgumentException(trialName + " is not an existing trial, so you can't remove it");
	}

	public static String[] getAllTrialNames() {
		String[] names = new String[trials.size()];
		for (int i=0; i<trials.size(); i++)
			names[i] = trials.get(i).getName();
		return names;
	}
}

