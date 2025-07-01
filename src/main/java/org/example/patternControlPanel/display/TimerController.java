package org.example.patternControlPanel.display;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.example.patternControlPanel.utils.VoidFunction;

import java.text.DecimalFormat;

public class TimerController extends AnimationTimer {
	private static DecimalFormat df = new DecimalFormat("00");
	private Stage stage;
	private VoidFunction closeFunction;
	private long startNanoTime;
	
	@Override
	public void start() {
		startNanoTime = System.nanoTime();
		super.start();
	}
	
	@FXML 
	private Label timeLabel;
	@FXML
	private Button stopButton;
	@FXML
	private void closeWindow() {
		if (stage == null)
			System.out.println("STAGE IS NULL FOR TIMER CONTROLLER");
		else
			stage.close();
		closeFunction.execute();
	}
	
	@Override
	public void handle(long now) {
		long nanoTimeRunning = now - startNanoTime;
		int sec = (int) (nanoTimeRunning / 1_000_000_000);
		String time = df.format(sec / 60) + ":" + df.format(sec % 60);
		timeLabel.setText(time);
	}
	
	public void setStage(Stage stage) {
		this.stage = stage;
	}
	public void setCloseFunction(VoidFunction closeFunction) {
		this.closeFunction = closeFunction;
	}
}
