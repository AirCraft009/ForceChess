package org.mxnik.forcechess.UI.menu;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.TrainingsView.TrainingsController;
import org.mxnik.forcechess.bot.BatchChessBot;


import java.io.IOException;

public class TrainPopup extends MenuPopup {
    private final TrainingsController controller = new TrainingsController();
    /**
     * The base for any popup-window in the menu scene
     *
     * @param primaryStage the primary stage used in the program
     */
    protected TrainPopup(Stage primaryStage) {
        super(primaryStage);
        Scene dummyScene = new Scene(new Group(), 1500, 700);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(50);
        grid.setBackground(Background.fill(Color.LIGHTGRAY));

        Label batchSizeLabel = new Label("Batch Size:");
        Constants.defaultStyleLabel(batchSizeLabel, dummyScene);
        grid.add(batchSizeLabel, 0, 0);

        TextField batchSizeField = new TextField(
                String.valueOf(BatchChessBot.BATCH_SIZE * 8)
        );
        batchSizeField.setFont(Font.font(
                null,
                FontWeight.BOLD,
                primaryStage.getWidth() / 120
        ));
        grid.add(batchSizeField, 1, 0);

        Label opponentText = new Label("Training net: ");
        Constants.defaultStyleLabel(opponentText, dummyScene);
        grid.add(opponentText, 0, 1);
        ChoiceBox<String> bots = super.getBotsList();
        if(bots == null){
            return;
        }
        grid.add(bots, 1, 1);
        Constants.defaultStyleChoiceBox(bots, dummyScene);

        Label iterCount = new Label("IterationCount: ");
        Constants.defaultStyleLabel(iterCount, dummyScene);
        grid.add(iterCount, 0, 2);
        TextField field = new TextField();
        field.setText("100000");
        field.setFont(Font.font(null, FontWeight.BOLD, null, primaryStage.getWidth()/120));
        grid.add(field, 1, 2);


        Label playDepthText = new Label("Play Depth: " + BatchChessBot.BATCH_SIZE);
        Constants.defaultStyleLabel(playDepthText, dummyScene);
        grid.add(playDepthText, 0, 3);
        Slider playDepthS = new Slider(BatchChessBot.BATCH_SIZE, BatchChessBot.BATCH_SIZE*64, BatchChessBot.BATCH_SIZE);
        playDepthS.setShowTickMarks(true);
        playDepthS.setShowTickLabels(true);
        playDepthS.setSnapToTicks(true);
        playDepthS.setMajorTickUnit(BatchChessBot.BATCH_SIZE*16);
        playDepthS.setMinorTickCount(15);
        playDepthS.valueProperty().addListener((observable, oldValue, newValue) -> {
            playDepthText.setText("Play Depth: " + Math.round(newValue.doubleValue()/64)*64);
        });
        grid.add(playDepthS, 1, 3);
        Constants.defaultStyleSlider(playDepthS, dummyScene);

        Button cancel = getButton("Cancel");
        cancel.setOnAction(e -> close());
        Constants.defaultStyleButton(cancel, dummyScene, false);
        grid.add(cancel, 0, 5);

        Label path = new Label("Name:");
        Constants.defaultStyleLabel(path, dummyScene);
        grid.add(path, 0, 4);

        TextField pathField = new TextField(
                "placeHolder"
        );
        pathField.setFont(Font.font(
                null,
                FontWeight.BOLD,
                primaryStage.getWidth() / 120
        ));
        grid.add(pathField, 1, 4);

        Button contButton = getContButton(primaryStage, pathField, bots, field, batchSizeField, playDepthS);
        Constants.defaultStyleButton(contButton, dummyScene, false);
        grid.add(contButton, 1, 5);

        Scene scene = new Scene(grid);
        setScene(scene);
        setTitle("PvB");
        show();
    }

    /**
     * @param primaryStage the Stage the match will be displayed in
     * @param bot the {@code ChoiceBox} used to choose the players opponent
     * @return the {@code Button} to start the PvB match
     */
    @NotNull
    private Button getContButton(
            Stage primaryStage,
            TextField nameF,
            ChoiceBox<String> bot,
            TextField iterCount,
            TextField batchSizeField,
            Slider playDepthS
    ) {
        Button contButton = getButton("Continue");

        contButton.setOnAction(e -> {

            int iterations;
            int batchSize;

            try {
                iterations = Integer.parseInt(iterCount.getText());
                batchSize = Integer.parseInt(batchSizeField.getText());
            } catch (NumberFormatException ex) {
                iterCount.setText("enter number");
                batchSizeField.setText("enter number");
                return;
            }

            int playDepth =
                    ((int) Math.round(playDepthS.getValue() / 64.0)) * 64;

            contButton.setDisable(true);

            controller.startTraining(
                    bot.getValue(),
                    nameF.getText(),
                    batchSize,
                    iterations,
                    playDepth
            );
        });

        return contButton;
    }
}
