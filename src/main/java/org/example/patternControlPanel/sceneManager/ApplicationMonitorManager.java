package org.example.patternControlPanel.sceneManager;

import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;
import org.example.patternControlPanel.pattern.MonitorFormat;

import java.util.function.Consumer;

public class ApplicationMonitorManager {

    private Consumer<MonitorFormat> monitorFormatConsumer;
    private MonitorFormat monitorFormat;

    public ApplicationMonitorManager(Stage stage) {
        monitorFormat = findCurrentMonitorFormat(stage.getX(), stage.getY());

        stage.xProperty().addListener((obs, oldX, newX) -> {
            handleStartMenuMoved(newX.doubleValue(), stage.getY());
        });
        stage.yProperty().addListener((obs, oldY, newY) -> {
            handleStartMenuMoved(stage.getX(), newY.doubleValue());
        });
    }

    public void setMonitorFormatConsumer(Consumer<MonitorFormat> monitorFormatConsumer) {
        this.monitorFormatConsumer = monitorFormatConsumer;
        monitorFormatConsumer.accept(monitorFormat);
    }

    private boolean inBounds(double x, double y, Rectangle2D bounds) {
        return x >= bounds.getMinX() && x <= bounds.getMaxX() && y >= bounds.getMinY() && y <= bounds.getMaxY();
    }

    private MonitorFormat findCurrentMonitorFormat(double x, double y) {
        for (int i=1; i< MonitorFormat.getNumScreens(); i++) {
            MonitorFormat monitorFormat = new MonitorFormat(i);
            if (inBounds(x, y, monitorFormat.getBounds()))
                return monitorFormat;
        }
        return null; // should never run
    }

    private void handleStartMenuMoved(double x, double y) {
        // still on same monitor
        if (inBounds(x, y, monitorFormat.getBounds()))
            return;

        // need to look for which monitor the screen moved to (this is def bad code but whatever, performance don't matter ;) )
        MonitorFormat newMonitorFormat = findCurrentMonitorFormat(x, y);
        monitorFormatConsumer.accept(newMonitorFormat);
        this.monitorFormat = newMonitorFormat;
    }
}
