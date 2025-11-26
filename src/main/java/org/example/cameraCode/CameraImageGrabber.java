package org.example.cameraCode;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;

// grabs the images from the camera and stores them in a list AS SOON as they are available
public class CameraImageGrabber {
    private static final int stableDtThreshold = 50, numStableFrames = 5;
    public VideoCapture cap;
    private volatile boolean running = false;
    private volatile Mat latestFrame = null;
    private Thread grabThread;
    private boolean connected;
    private int numFramesGrabbed;
    private final ArrayList<Double> previousDts;

    public CameraImageGrabber(int devicePort) {
        cap = new VideoCapture(devicePort);
        connected = cap.isOpened();
        previousDts = new ArrayList<>();
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
        grabThread = new Thread(this::grabLoop, "frame grabber thread");
        grabThread.setDaemon(true);
        grabThread.start();
    }

    public void stopGrabbing() {
        numFramesGrabbed = 0;
        running = false;
        if (grabThread != null) {
            try {
                grabThread.join();
            } catch (InterruptedException ignored) {}
        }
    }

    private void grabLoop() {
        double lastUpdateTime = System.nanoTime() * 1e-6;
        while (running) {
            double currentTime = System.nanoTime() * 1e-6;
            double dt = currentTime - lastUpdateTime;
            previousDts.add(dt);
            lastUpdateTime = currentTime;
            if (previousDts.size() > numStableFrames)
                previousDts.removeFirst();

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

    public static String testTrialPath = "C:/Users/justi/Documents/omr images/raw images/wt1_cw_highw_lowspeed";
    private int testGrabImageNum = 1;
    private void grabLoopTesting() {
        double lastUpdateTime = System.nanoTime() * 1e-6;
        while (running) {
            double currentTime = System.nanoTime() * 1e-6;
            double dt = currentTime - lastUpdateTime;
            previousDts.add(dt);
            lastUpdateTime = currentTime;
            if (previousDts.size() > numStableFrames)
                previousDts.removeFirst();

            Mat bgra = Imgcodecs.imread(testTrialPath + "/" + testGrabImageNum + ".png", Imgcodecs.IMREAD_UNCHANGED);

            // Convert BGRA → BGR if needed
            Mat bgr = new Mat();
            if (bgra.channels() == 4)
                Imgproc.cvtColor(bgra, bgr, Imgproc.COLOR_BGRA2BGR);
            else
                bgr = bgra;

            latestFrame = bgr;
            numFramesGrabbed++;
            testGrabImageNum++;
        }
    }

    public boolean reachedStableFPS() {
        if (previousDts.size() < numStableFrames)
            return false;
        for (Double previousDt : previousDts)
            if (previousDt > stableDtThreshold)
                return false;
        return true;
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

