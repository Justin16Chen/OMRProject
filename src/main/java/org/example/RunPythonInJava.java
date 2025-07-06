package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunPythonInJava {
    public static void main(String[] args) {
        String pythonPath = "C:\\Users\\justi\\anaconda3\\envs\\omrEnv\\python.exe";
        String scriptPath = "python\\test.py";

        ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);

        try {
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );
            String line;
            while((line = reader.readLine()) != null)
                System.out.println(line);

            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
            );
            while((line = errorReader.readLine()) != null)
                System.err.println("Error: " + line);

            int exitCode = process.waitFor();
            System.out.println("Exited with code: " + exitCode);
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("hellow world");
    }
}
