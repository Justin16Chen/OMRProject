package org.example.integration;

import org.example.trialControlPanel.trialConfig.TrialConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

// handles reading and writing to programInfo.json file (this is how the java and python code communicates with each other)
public class ProgramInfoManager {
    private static final String PROGRAM_INFO_FILE_PATH = "liveData/programInfo.json";

    private JSONObject readJSONFromFile() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(PROGRAM_INFO_FILE_PATH)));
            return new JSONObject(content);
        } catch (IOException e) {
            throw new RuntimeException("FAILED TO GET JSON OBJECT AT " + PROGRAM_INFO_FILE_PATH);
        }
    }
    public int getFPS() {
        return readJSONFromFile().getInt("fps");
    }
    public void startProgram() {
        JSONObject json = readJSONFromFile();
        json.put("programActive", true);
        json.put("stopEarly", false);
        saveJSONToFile(json, "FAILED TO START PROGRAM");
    }

    public void stopProgram() {
        JSONObject json = readJSONFromFile();
        json.put("programActive", false);

        saveJSONToFile(json, "FAILED TO STOP PROGRAM");
    }
    public void activateExperiments(ArrayList<TrialConfig> trials) {
        JSONObject json = readJSONFromFile();
        json.put("stopEarly", false);
        JSONArray experimentsJson = new JSONArray();
        for (TrialConfig trial : trials) {
            JSONObject trialJsonObject = new JSONObject();
            trialJsonObject.put("name", trial.getName());
            trialJsonObject.put("expectedImages", trial.getTestTime() * getFPS());
            trialJsonObject.put("completed", false);
            experimentsJson.put(trialJsonObject);
        }
        json.put("experiments", experimentsJson);

        saveJSONToFile(json, "FAILED TO ACTIVATE EXPERIMENTS");
    }
    public void stopExperimentsEarly() {
        JSONObject json = readJSONFromFile();
        json.put("stopEarly", true);

        saveJSONToFile(json, "FAILED TO STOP EXPERIMENTS EARLY");
    }
    public void completeAllExperiments() {
        JSONObject json = readJSONFromFile();
        JSONArray experiments = json.getJSONArray("experiments");
        for (int i=0; i<experiments.length(); i++)
            experiments.getJSONObject(i).put("completed", true);

        saveJSONToFile(json, "FAILED TO COMPLETE ALL EXPERIMENTS");
    }
    public void completeExperiment(String name) {
        JSONObject json = readJSONFromFile();
        JSONArray experiments = json.getJSONArray("experiments");
        for (int i=0; i<experiments.length(); i++)
            if (experiments.getJSONObject(i).get("name").equals(name)) {
                experiments.getJSONObject(i).put("completed", true);
                break;
            }

        saveJSONToFile(json, "FAILED TO COMPLETE EXPERIMENT");
    }


    private void saveJSONToFile(JSONObject json, String errorMessage) {
        try(FileWriter file = new FileWriter(PROGRAM_INFO_FILE_PATH)) {
            file.write(json.toString(4));
        } catch (IOException e) {
            System.out.println(errorMessage);
        }
    }
}
