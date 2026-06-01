package org.mxnik.forcechess.UI.menu;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.UI.ChessControllView.ChessView;
import org.mxnik.forcechess.bot.BatchChessBot;

import java.util.Random;

public class PvBPopup extends MenuPopup {
    /**
     * Creates a popup-window for starting a player vs bot match
     * @param primaryStage the Stage the match will be displayed in
     */
    protected PvBPopup(Stage primaryStage) {
        super(primaryStage);
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(50);
        grid.setBackground(Background.fill(Color.LIGHTGRAY));

        Label boardText = new Label("Board: ");
        grid.add(boardText, 0, 0);
        ChoiceBox<String> board = boardCB(8);
        board.getSelectionModel().select("default");
        grid.add(board, 1, 0);

        Label opponentText = new Label("Opponent: ");
        grid.add(opponentText, 0, 1);
        ChoiceBox<String> bots = super.getBotsList();
        bots.getSelectionModel().selectFirst();
        grid.add(bots, 1, 1);

        Label playDepthText = new Label("Play Depth: " + BatchChessBot.BATCH_SIZE);
        grid.add(playDepthText, 0, 2);
        Slider playDepthS = new Slider(BatchChessBot.BATCH_SIZE, BatchChessBot.BATCH_SIZE*8, BatchChessBot.BATCH_SIZE);
        playDepthS.setShowTickMarks(true);
        playDepthS.setShowTickLabels(true);
        playDepthS.setSnapToTicks(true);
        playDepthS.setMajorTickUnit(BatchChessBot.BATCH_SIZE);
        playDepthS.setMinorTickCount(0);
        playDepthS.valueProperty().addListener((observable, oldValue, newValue) -> {
            playDepthText.setText("Play Depth: " + Math.round(newValue.doubleValue()/64)*64);
        });
        grid.add(playDepthS, 1, 2);

        Button cancel = getButton("Cancel");
        cancel.setOnAction(e -> close());
        grid.add(cancel, 0, 3);

        Button contButton = getContButton(primaryStage, board, bots, playDepthS);
        grid.add(contButton, 1, 3);

        Scene scene = new Scene(grid);
        setScene(scene);
        setTitle("PvP");
        show();
    }

    /**
     * @param primaryStage the Stage the match will be displayed in
     * @param board the {@code ChoiceBox} used to choose the board-layout
     * @param bot the {@code ChoiceBox} used to choose the players opponent
     * @return the {@code Button} to start the PvB match
     */
    @NotNull
    private Button getContButton(Stage primaryStage, ChoiceBox<String> board, ChoiceBox<String> bot, Slider playDepthS) {
        Button contButton = getButton("Continue");
        contButton.setOnAction(e -> {
            close();
            String fen = FenProperties.getFenStr(board.getValue());
            System.out.println(fen);
            int sideLen = 8;
//            Integer.parseInt(String.valueOf(fen.charAt(fen.length()-1)));
            try {
                if(new Random().nextBoolean())
                    new ChessView(primaryStage, fen, sideLen, bot.getValue(), null, (int)playDepthS.getValue());
                else
                    new ChessView(primaryStage, fen, sideLen, null, bot.getValue(), (int)playDepthS.getValue());
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex);
            }
        });
        return contButton;
    }
}
