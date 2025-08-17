package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

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
        String pythonPath = "C:\\Users\\justi\\anaconda3\\envs\\omrEnv\\python.exe";

        ProcessBuilder pb = new ProcessBuilder(pythonPath, "-m", "src.omrEval");
        pb.directory(new File("C:\\Users\\justi\\Documents\\GitHub\\OMRProject\\src\\pythonCode")); // setting working directory to be the project root
        pb.redirectErrorStream(true); // merges error stream with normal output stream so that one buffered reader receives both errors and print statements

        return pb.start();
    }
}
