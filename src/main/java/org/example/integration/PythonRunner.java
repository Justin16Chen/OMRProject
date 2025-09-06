package org.example.integration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;

public class PythonRunner {

    public void start() throws IOException, InterruptedException {
        Process p = getProcess();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
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

        ProcessBuilder pb = new ProcessBuilder(pythonPath, "-u", "-m", "src.omrEval");
        pb.directory(new File(pythonWorkingDir)); // setting working directory to be the project root
        pb.redirectErrorStream(true); // merges error stream with normal output stream so that one buffered reader receives both errors and print statements

        return pb.start();
    }

    public static void main(String[] args) {
//        System.out.println("available cores: " + Runtime.getRuntime().availableProcessors());
//        System.exit(0);

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
        try (ServerSocket serverSocket = new ServerSocket(65432)) {
            Socket clientSocket = serverSocket.accept();
            DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
            int i = 0;
            while (true) {
                i++;
                try {
                    int length = dataIn.readInt();
                    double time = System.currentTimeMillis();
                    byte[] imgBytes = new byte[length];
                    dataIn.readFully(imgBytes);

                    ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
//                    System.out.println(length);
//                    System.out.println();
//                    System.out.println();
//                    System.out.println(Arrays.toString(imgBytes));
//                    System.out.println();
//                    System.out.println();
                    BufferedImage img = ImageIO.read(bais);
                    System.out.println("time to read img: " + (System.currentTimeMillis() - time) / 1000);

                    if (img == null) {
                        System.out.println("IMAGE IS NULL");
                        System.exit(1);
                    }

                    File output = new File("C:\\Users\\justi\\Documents\\GitHub\\OMRProject\\liveData\\test " + i + ".png");
                    ImageIO.write(img, "png", output);
                    System.out.println("saved python image");
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
