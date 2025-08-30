package org.example.integration;

import org.example.trialControlPanel.trialConfig.Experiment;
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

    private JSONObject readProgramInfoJSON() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(PROGRAM_INFO_FILE_PATH)));
            return new JSONObject(content);
        } catch (IOException e) {
            throw new RuntimeException("FAILED TO GET JSON OBJECT AT " + PROGRAM_INFO_FILE_PATH);
        }
    }
    public int getFPS() {
        return readProgramInfoJSON().getInt("fps");
    }
    public int getExpectedImages(int index, String experimentName) {
        JSONArray experiments = readProgramInfoJSON().getJSONArray("experiments");
        for (int i=0; i<experiments.length(); i++)
            if (experiments.getJSONObject(i).getString("name").equals(getExperimentFileName(index, experimentName)))
                return experiments.getJSONObject(i).getInt("expectedImages");
        return -1;
    }
    public int getSocketPort() {
        return readProgramInfoJSON().getInt("PORT");
    }
    public void startProgram() {
        JSONObject json = readProgramInfoJSON();
        json.put("programRunning", true);
        json.put("stopEarly", false);
        saveJSONToFile(json, "FAILED TO START PROGRAM");
    }

    public void stopProgram() {
        JSONObject json = readProgramInfoJSON();
        json.put("programRunning", false);

        saveJSONToFile(json, "FAILED TO STOP PROGRAM");
    }
    private String getExperimentFileName(int index, String experimentName) {
        return index + " - " + experimentName;
    }
    public void activateExperiments(ArrayList<Experiment> experiments) {
        JSONObject json = readProgramInfoJSON();
        json.put("stopEarly", false);
        JSONArray experimentsJson = new JSONArray();
        for (int i=0; i<experiments.size(); i++) {
            Experiment experiment = experiments.get(i);
            JSONObject trialJsonObject = new JSONObject();
            trialJsonObject.put("name", getExperimentFileName(i, experiment.getName()));
            trialJsonObject.put("expectedImages", experiment.getTestTime() * getFPS());
            trialJsonObject.put("completed", false);
            experimentsJson.put(trialJsonObject);
        }
        json.put("experiments", experimentsJson);

        saveJSONToFile(json, "FAILED TO ACTIVATE EXPERIMENTS");
    }
    public void stopExperimentsEarly() {
        JSONObject json = readProgramInfoJSON();
        json.put("stopEarly", true);

        saveJSONToFile(json, "FAILED TO STOP EXPERIMENTS EARLY");
    }
    public void completeAllExperiments() {
        JSONObject json = readProgramInfoJSON();
        JSONArray experiments = json.getJSONArray("experiments");
        for (int i=0; i<experiments.length(); i++)
            experiments.getJSONObject(i).put("completed", true);

        saveJSONToFile(json, "FAILED TO COMPLETE ALL EXPERIMENTS");
    }
    public void completeExperiment(int index, String name) {
        JSONObject json = readProgramInfoJSON();
        JSONArray experiments = json.getJSONArray("experiments");
        for (int i=0; i<experiments.length(); i++) {
            String rawExperimentName = experiments.getJSONObject(i).getString("name");
            if (getExperimentFileName(index, rawExperimentName).equals(name)) {
                experiments.getJSONObject(i).put("completed", true);
                break;
            }
        }

        saveJSONToFile(json, "FAILED TO COMPLETE EXPERIMENT");
    }

    public String getLastCameraOutputPath() {
        return readProgramInfoJSON().getString("cameraOutputBase");
    }
    public void setCameraOutputPath(String path) {
        JSONObject json = readProgramInfoJSON();
        json.put("cameraOutputBase", path);
        saveJSONToFile(json, "FAILED TO SET CAMERA OUTPUT BASE");
    }
    public String getLastVisualizedOutputPath() {
        return readProgramInfoJSON().getString("visualizedOutputBase");
    }
    public void setVisualizedOutputPath(String path) {
        JSONObject json = readProgramInfoJSON();
        json.put("visualizedOutputBase", path);
        saveJSONToFile(json, "FAILED TO SET VISUALIZED OUTPUT BASE");
    }


    private void saveJSONToFile(JSONObject json, String errorMessage) {
        try(FileWriter file = new FileWriter(PROGRAM_INFO_FILE_PATH)) {
            file.write(json.toString(4));
        } catch (IOException e) {
            System.out.println(errorMessage);
        }
    }
}
