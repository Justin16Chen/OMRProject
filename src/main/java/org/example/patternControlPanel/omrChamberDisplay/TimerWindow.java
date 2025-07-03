package org.example.patternControlPanel.omrChamberDisplay;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.patternControlPanel.pattern.MonitorFormat;
import org.example.patternControlPanel.utils.VoidFunction;

import java.io.IOException;

public class TimerWindow extends Stage {
	private TimerController controller;
	
	public TimerWindow(MonitorFormat monitorFormat) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/patternControlPanelFXML/Timer.fxml"));
	    Parent root;
		try {
			root = loader.load();
			
			controller = loader.getController();
			controller.setStage(this);
		    			
		    setTitle("PatternDisplay");
		    setScene(new Scene(root, 300, 200));
		    Rectangle2D bounds = monitorFormat.getBounds();
		    setX(bounds.getMinX() + bounds.getWidth() / 2 - getWidth() / 2);
		    setY(bounds.getMinY() + bounds.getHeight() / 2 - getHeight() / 2);
		    show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void startTimer() {
		controller.start();
	}
	public void setCloseFunction(VoidFunction closeFunction) {
		controller.setCloseFunction(closeFunction);
	}
}
