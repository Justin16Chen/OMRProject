package org.example.integration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class PythonRunner {
    public static final int PORT = 65432;

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

    public static void main(String[] args) {
        PythonRunner pythonRunner = new PythonRunner();
        Thread pythonThread = new Thread(() -> {
            try {
                pythonRunner.start();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        pythonThread.start();

        System.out.println("starting java socket");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            Socket clientSocket = serverSocket.accept();
            DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
            while (true) {
                try {
                    int length = dataIn.readInt();
                    double time = System.currentTimeMillis();
                    byte[] imgBytes = new byte[length];
                    dataIn.readFully(imgBytes);

                    ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                    BufferedImage img = ImageIO.read(bais);
                    System.out.println("time to read img: " + (System.currentTimeMillis() - time) / 1000);
                    System.out.println("received python image");
                } catch(EOFException eof) {
                    System.out.println("client disconnected");
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
