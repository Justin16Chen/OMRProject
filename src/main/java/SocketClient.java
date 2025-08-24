
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class SocketClient {
    public static void main(String[] args) {
        try {

            double before = System.currentTimeMillis();
            File file = new File("C:\\Users\\justi\\Documents\\GitHub\\OMRProject\\liveData\\cameraImages\\2.png");

            BufferedImage image = ImageIO.read(file);
            System.out.println("time to load: " + (System.currentTimeMillis() - before) /1000);     //TIME TO READ IMAGE ~0.08 SECONDS
            if (image != null)
                System.out.println("image successfully loaded");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
//        try {
//            double before = System.currentTimeMillis();
//            String content = new String(Files.readAllBytes(Paths.get("C:\\Users\\justi\\Documents\\GitHub\\OMRProject\\liveData\\programInfo.json")));
//            JSONObject json = new JSONObject(content);
//            double fps = json.getInt("fps");
//            System.out.println("time to read from json: " + (System.currentTimeMillis() - before) / 1000); TIME TO READ FROM JSON ~ 0.04 SECONDS
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }

        // READING FROM JSON IN PYTHON IS QUICK: ~ 0.0003s, to write is ~ 0.0009s
        // but cannot send visualized images via json because reading from java is slow
        // so we should use sockets for that
        // saving images in java takes ~ 1.5s
        // so we should use sockets everytime we send camera data between java and python program


//        String host = "127.0.0.1"; // localhost
//        int port = 65432;
//        try (Socket socket = new Socket(host, port)) {
//            OutputStream out = socket.getOutputStream();
//            PrintWriter w = new PrintWriter(out, true);
//            w.println("Hello from Java Client");
//
//            // receive response
//            BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(socket.getInputStream())
//            );
//            char[] buffer = new char[1024];
//            int read = reader.read(buffer);
//            String response = new String(buffer, 0, read);
//            System.out.println("Received from server: " + response);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
