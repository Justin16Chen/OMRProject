package org.example.integration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;

public class PythonRunner {

    private final Process process;
    public PythonRunner() {
        try {
            this.process = getProcess();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void start() {
        if (process == null)
            throw new IllegalStateException("PROCESS IS NULL IN PYTHON RUNNER");

         new Thread(() -> {
            try {
                run();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("python thread finished");
        }, "python runner thread").start();
    }
    private void run() throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty())
                System.out.println("python line: " + line);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0)
            System.out.println("PYTHON PROCESS EXITED WITH CODE " + exitCode
                    + " - the java side will hang on 'Loading...' because nothing will connect to the sockets");
    }

    public void stopRunning() {
        if (process != null)
            process.destroy();
    }

    private Process getProcess() throws IOException {

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("local.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            System.out.println("FAILED TO OPEN LOCAL.PROPERTIES FILE");
            e.printStackTrace();
        }
        String pythonPath = unquote(properties.getProperty("pythonEnv.path"));
        String pythonWorkingDir = unquote(properties.getProperty("pythonWorkingDirectory.path"));

        ProcessBuilder pb = new ProcessBuilder(pythonPath, "-u", "-m", "src.omrEval");
        pb.directory(new File(pythonWorkingDir)); // setting working directory to be the project root
        pb.redirectErrorStream(true); // merges error stream with normal output stream so that one buffered reader receives both errors and print statements
        configureCondaEnvironment(pb, new File(pythonPath).getParentFile());

        return pb.start();
    }

    private static String unquote(String s) {
        if (s == null)
            return null;
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\""))
            s = s.substring(1, s.length() - 1);
        return s;
    }

    // launching <env>/python.exe directly skips conda activation, so the env's native DLL directories are
    // never added to PATH. numpy then dies during import with a bare "module not found" loader error - no
    // traceback, no output - and the java side waits on a socket connection that will never happen.
    private static void configureCondaEnvironment(ProcessBuilder pb, File envRoot) {
        if (envRoot == null)
            return;

        String[] dllDirs = {"", "Library\\mingw-w64\\bin", "Library\\usr\\bin", "Library\\bin", "Scripts", "bin"};
        StringBuilder path = new StringBuilder();
        for (String dir : dllDirs) {
            File f = dir.isEmpty() ? envRoot : new File(envRoot, dir);
            if (f.isDirectory())
                path.append(f.getAbsolutePath()).append(File.pathSeparator);
        }

        Map<String, String> env = pb.environment();
        // environment variable names are case-insensitive on windows but the map is not, so find the real key
        String pathKey = "PATH";
        for (String key : env.keySet())
            if (key.equalsIgnoreCase("PATH")) {
                pathKey = key;
                break;
            }
        env.put(pathKey, path + env.getOrDefault(pathKey, ""));

        // conda's Library/bin ships its own OpenMP runtime that clashes with the one bundled in pip's torch;
        // without this the torch import aborts the interpreter with "OMP: Error #15"
        env.putIfAbsent("KMP_DUPLICATE_LIB_OK", "TRUE");
    }

    public static void main(String[] args) {
//        System.out.println("available cores: " + Runtime.getRuntime().availableProcessors());
//        System.exit(0);

        PythonRunner pythonRunner = new PythonRunner();
        Thread pythonThread = new Thread(() -> {
            try {
                pythonRunner.run();
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
