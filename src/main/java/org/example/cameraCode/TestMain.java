package org.example.cameraCode;

public class TestMain {
    private static final int cameraFPS = 30;

    public static void main(String[] args) {
        startCameraTracking();
    }

    private static void startCameraTracking() {
        Thread cameraThread = new Thread(() -> {
            double time = System.currentTimeMillis();
            double lastUpdateTime = time;
            double millisPerFrame = 1000.0 / cameraFPS;

            CameraManager cm = new CameraManager();
            cm.setRecording(true);

            while (true) {
                time = System.currentTimeMillis();
                if(time - lastUpdateTime > millisPerFrame) {
                    cm.update();
                    lastUpdateTime = time;
                }
            }
        });
        cameraThread.start();
    }
}
