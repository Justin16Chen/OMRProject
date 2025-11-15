package org.example.cameraCode;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

// grabs the images from the camera and stores them in a list AS SOON as they are available
public class CameraImageGrabber {
    public VideoCapture cap;
    private volatile boolean running = false;
    private volatile Mat latestFrame = null;
    private Thread grabThread;
    private boolean connected;
    private int numFramesGrabbed;

    public CameraImageGrabber(int devicePort) {
        cap = new VideoCapture(devicePort);
        connected = cap.isOpened();
    }

    public boolean isConnected() {
        return connected;
    }

    public void attemptReconnect(int devicePort) {
        cap = new VideoCapture(devicePort);
        connected = cap.isOpened();
    }

    public void startGrabbing() {
        if (running) return;

        numFramesGrabbed = 0;
        running = true;
        grabThread = new Thread(this::grabLoop, "FrameGrabberThread");
        grabThread.setDaemon(true);
        grabThread.start();
    }

    public void stop() {
        numFramesGrabbed = 0;
        running = false;
        if (grabThread != null) {
            try {
                grabThread.join();
            } catch (InterruptedException ignored) {}
        }
    }

    private void grabLoop() {
        while (running) {
            Mat frame = new Mat();
            if (cap.read(frame)) {
                latestFrame = frame;  // Overwrite previous
                numFramesGrabbed++;
            } else {
                // If read fails, sleep to avoid burning CPU
                try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Returns the most recent frame captured by the grabber.
     * IMPORTANT: If you plan to store this frame, call `clone()`.
     */
    public Mat getLatestFrame() {
        return latestFrame;
    }

    public int getNumFramesGrabbed() {
        return numFramesGrabbed;
    }
}

