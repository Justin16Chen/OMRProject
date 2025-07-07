package org.example.trialControlPanel.utils;

public class ElapsedTime {

    private long startTime;       // Stores the start time in nanoseconds
    private long endTime;         // Stores the end time in nanoseconds
    private long pausedTime;      // Stores the accumulated paused time
    private long pauseStartTime;  // Tracks when the timer was paused
    private boolean running;      // Flag to indicate if the timer is running
    private boolean paused;       // Flag to indicate if the timer is paused

    /**
     * Starts the timer by recording the current time.
     */
    public void start() {
        startTime = System.nanoTime();
        running = true;
        paused = false;
        pausedTime = 0;
    }

    /**
     * Stops the timer and records the end time.
     */
    public void stop() {
        if (!running) {
            throw new IllegalStateException("Timer is not running.");
        }
        if (paused) {
            unpause(); // Resume before stopping
        }
        endTime = System.nanoTime();
        running = false;
    }

    public boolean isStarted() {
    	return running;
    }
    public boolean isPaused() {
    	return paused;
    }
    /**
     * Pauses the timer.
     */
    public void pause() {
        if (!running) 
            throw new IllegalStateException("Timer is not running.");
        if (paused)
        	return;
        pauseStartTime = System.nanoTime();
        paused = true;
    }

    /**
     * Resumes the timer after a pause.
     */
    public void unpause() {
        if (!running) 
            throw new IllegalStateException("Timer is not running.");
        if (!paused)
        	return;
        pausedTime += System.nanoTime() - pauseStartTime;
        paused = false;
    }

    /**
     * Returns the elapsed time in nanoseconds, excluding paused time.
     */
    public long getElapsedTimeNano() {
    	if (!isStarted())
    		return 0;
        if (paused) {
            return pauseStartTime - startTime - pausedTime;
        } else if (running) {
            return System.nanoTime() - startTime - pausedTime;
        } else {
            return endTime - startTime - pausedTime;
        }
    }

    /**
     * Returns the elapsed time in milliseconds.
     */
    public long getElapsedTimeMillis() {
        return getElapsedTimeNano() / 1_000_000;
    }

    /**
     * Returns the elapsed time in seconds.
     */
    public double getElapsedTimeSeconds() {
        return getElapsedTimeNano() / 1_000_000_000.0;
    }

    /**
     * Resets the timer.
     */
    public void reset() {
        startTime = 0;
        endTime = 0;
        pausedTime = 0;
        pauseStartTime = 0;
        running = false;
        paused = false;
    }
}
