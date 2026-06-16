package org.mxnik.forcechess.UI.TrainingsView;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.mxnik.forcechess.UI.Constants;

import java.awt.Desktop;
import java.net.URI;

public class TrainingsView extends Stage {

    private static final String DASHBOARD_URL =
            "http://localhost:9000/train/overview";

    private final Label statusLabel = new Label("Idle");
    final Button openDashboard;

    private final TrainingsController controller;

    public TrainingsView(TrainingsController controller) {
        this.controller = controller;

        Scene dummy = new Scene(new Group(), 1500, 700);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));
        grid.setHgap(20);
        grid.setVgap(20);

        grid.setBackground(Background.fill(Color.LIGHTGRAY));

        Label title = new Label("Training Running");
        Constants.defaultStyleLabel(title, dummy);
        grid.add(title, 0, 0, 2, 1);

        Constants.defaultStyleLabel(statusLabel, dummy);
        grid.add(statusLabel, 0, 1, 2, 1);

        openDashboard = new Button("Open Dashboard");
        openDashboard.setDisable(true);
        Constants.defaultStyleButton(openDashboard, dummy, false);
        openDashboard.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI(DASHBOARD_URL));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        grid.add(openDashboard, 0, 2, 2, 1);

        Button back = new Button("Back to Menu");
        Constants.defaultStyleButton(back, dummy, false);
        back.setOnAction(e -> close());

        Button stop = new Button("Stop Training");
        Constants.defaultStyleButton(stop, dummy, false);
        stop.setOnAction(e -> controller.stopTraining());

        grid.add(back, 0, 3);
        grid.add(stop, 1, 3);


        setScene(new Scene(grid, 500, 300));
        setTitle("Training");
    }

    public void setStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }
}