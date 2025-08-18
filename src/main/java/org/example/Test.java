package org.example;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.example.trialControlPanel.monitorInfo.MonitorFormat;

public class Test extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        for (int i=1; i<MonitorFormat.getNumScreens()+1; i++) {
            MonitorFormat mf = new MonitorFormat(i);
            System.out.println(mf);
            System.out.println(mf.getBounds());

            Canvas canvas = new Canvas(mf.getBounds().getWidth(), mf.getBounds().getHeight());
            Scene scene = new Scene(new Group(canvas));
            Stage stage = new Stage();
            stage.setX(mf.getBounds().getMinX());
            stage.setY(mf.getBounds().getMinY());
            stage.setScene(scene);
            stage.show();
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.BLACK);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.setStroke(Color.WHITE);
            g.setFont(new Font(50));
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.strokeText(stage.getX() + ", " + stage.getY(), 70, 70);
            stage.xProperty().addListener((obs, old, newVal) -> {
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                g.strokeText(stage.getX() + ", " + stage.getY(), 70, 70);
            });
        }
    }
}
