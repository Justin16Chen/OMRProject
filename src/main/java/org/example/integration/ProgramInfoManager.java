package org.example.integration;

import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// handles reading and writing to programInfo.json file (this is how the java and python code communicates with each other)
public class ProgramInfoManager {
    private static final String PROGRAM_INFO_FILE_PATH = "liveData/programInfo.json", TRIAL_ACTIVE_KEY = "trialActive", TEST_TIME_KEY = "testTime", REST_TIME_KEY = "restTime";

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
    public void activateTrial(double testTime, double restTime) {
        JSONObject json = readJSONFromFile();
        json.put(TRIAL_ACTIVE_KEY, true);
        json.put(TEST_TIME_KEY, testTime);
        json.put(REST_TIME_KEY, restTime);

        try(FileWriter file = new FileWriter(PROGRAM_INFO_FILE_PATH)) {
            file.write(json.toString(4));
        } catch (IOException e) {
            System.out.println("FAILED TO ACTIVATE TRIAL");
        }
    }
    public void deactivateTrial() {
        JSONObject json = readJSONFromFile();
        json.put(TRIAL_ACTIVE_KEY, false);
        json.put(TEST_TIME_KEY, "-1");
        json.put(REST_TIME_KEY, "-1");

        try(FileWriter file = new FileWriter(PROGRAM_INFO_FILE_PATH)) {
            file.write(json.toString(4));
        }  catch(IOException e) {
            System.out.println("FAILED TO DE-ACTIVATE TRIAL");
        }
    }
}
