package org.example.integration;

import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// handles reading and writing to programInfo.json file (this is how the java and python code communicates with each other)
public class JsonManager {
    private static final String PROGRAM_INFO_FILE_PATH = "liveData/programInfo.json";
    public static final int VISUALIZED_IMAGES_RECEIVER_PORT = 65432, RAW_IMAGES_SENDER_PORT = 65433;

    private JSONObject readProgramInfoJSON() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(PROGRAM_INFO_FILE_PATH)));
            return new JSONObject(content);
        } catch (IOException e) {
            throw new RuntimeException("FAILED TO GET JSON OBJECT AT " + PROGRAM_INFO_FILE_PATH);
        }
    }

    public String getLastParentFolderPath() {
        return readProgramInfoJSON().getString("parentFolderPath");
    }
    public void setParentFolderPath(String path) {
        JSONObject json = readProgramInfoJSON();
        json.put("parentFolderPath", path);
        saveJSONToFile(json, "FAILED TO SET PARENT FOLDER FOR EXPORTING");
    }

    private void saveJSONToFile(JSONObject json, String errorMessage) {
        try(FileWriter file = new FileWriter(PROGRAM_INFO_FILE_PATH)) {
            file.write(json.toString(4));
        } catch (IOException e) {
            System.out.println(errorMessage);
        }
    }
}
