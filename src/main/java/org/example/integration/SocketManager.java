package org.example.integration;

import org.example.trialControlPanel.parentClasses.Core;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketManager {
    private final Core core;
    private DataOutputStream outputStream;

    public SocketManager(Core core) {
        this.core = core;
    }
    public void connectOutputStream() throws IOException {
        ServerSocket serverSocket = new ServerSocket(JsonManager.RAW_IMAGES_SENDER_PORT);
        Socket clientSocket = serverSocket.accept();
        outputStream = new DataOutputStream(clientSocket.getOutputStream());
        System.out.println("camera connected to python client");
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
}
