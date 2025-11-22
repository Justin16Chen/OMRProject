package org.example.integration;

import org.example.trialControlPanel.parentClasses.Core;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class SocketManager {
    private DataOutputStream outputStream;
    private ServerSocket serverSocket;
    private Socket clientSocket;

    public SocketManager() {
    }

    public void connectOutputStream() throws IOException {
        serverSocket = new ServerSocket(JsonManager.RAW_IMAGES_SENDER_PORT);
        try {
            clientSocket = serverSocket.accept();   // <— will break if serverSocket.close() is called
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("camera connected to python client");
        } catch (SocketException e) {
            System.out.println("error connecting socket - probably closed");
        }
    }

    public void writeHeaderData(int width, int height, int fps) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(12);
        header.putInt(width);
        header.putInt(height);
        header.putInt(fps);
        outputStream.write(header.array());
        outputStream.flush();
        System.out.println("camera data sent to python client; width: " + width + ", height: " + height);
    }

    public void writeData(byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }

    public void flush() throws IOException {
        outputStream.flush();
    }

    public void stop() {
        // safely close everything
        try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {}
        try { if (clientSocket != null) clientSocket.close(); } catch (IOException ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }
}

