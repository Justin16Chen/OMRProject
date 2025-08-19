package org.example.integration;

import java.io.*;
import java.util.Properties;

public class PythonRunner {
    public void start() throws IOException, InterruptedException {
        Process p = getProcess();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("py:" + line);
        }
        int exitCode = p.waitFor(); // thread blocking function call; java will not continue until python script finishes
        System.out.println("Exited with " + exitCode);
    }

    private Process getProcess() throws IOException {

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("local.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            System.out.println("FAILED TO OPEN LOCAL.PROPERTIES FILE");
            e.printStackTrace();
        }
        String pythonPath = properties.getProperty("pythonEnv.path");
        String pythonWorkingDir = properties.getProperty("pythonWorkingDirectory.path");

        ProcessBuilder pb = new ProcessBuilder(pythonPath, "-m", "src.omrEval");
        pb.directory(new File(pythonWorkingDir)); // setting working directory to be the project root
        pb.redirectErrorStream(true); // merges error stream with normal output stream so that one buffered reader receives both errors and print statements

        return pb.start();
    }
}
