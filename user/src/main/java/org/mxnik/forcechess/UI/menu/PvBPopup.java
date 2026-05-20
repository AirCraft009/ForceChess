package org.mxnik.forcechess.UI.menu;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.UI.ChessControllView.ChessScene;

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
        ChoiceBox<String> board = boardCB();
        board.getSelectionModel().select("default");
        grid.add(board, 1, 0);

        Label opponentText = new Label("Opponent: ");
        grid.add(opponentText, 0, 1);
        ChoiceBox<String> bots = super.getBotsList();
        bots.getSelectionModel().selectFirst();
        grid.add(bots, 1, 1);

        Button cancel = getButton("Cancel");
        cancel.setOnAction(e -> close());
        grid.add(cancel, 0, 2);

        Button contButton = getContButton(primaryStage, board, bots);
        grid.add(contButton, 1, 2);

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
    private Button getContButton(Stage primaryStage, ChoiceBox<String> board, ChoiceBox<String> bot) {
        Button contButton = getButton("Continue");
        contButton.setOnAction(e -> {
            close();
            String fen = FenProperties.getFenStr(board.getValue());
            int sideLen = Integer.parseInt(String.valueOf(fen.charAt(fen.length()-1)));
            try {
                if(new Random().nextBoolean())
                    new ChessScene(primaryStage, fen, sideLen, bot.getValue(), null);
                else
                    new ChessScene(primaryStage, fen, sideLen, null, bot.getValue());
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex);
            }
        });
        return contButton;
    }

    /**
     * @return a {@code ChoiceBox<String>} containing all 8x8 board layouts (Only possibility for bots)
     */
    private ChoiceBox<String> boardCB(){
        ChoiceBox<String> board = new ChoiceBox<>();
        FenProperties.load();
        ObservableList<String> items = FXCollections.observableArrayList(FenProperties.fenNames);
        for(int i = 0; i < items.size(); i++) {
            String correspFen = FenProperties.getFenStr(items.get(i));
            if(correspFen.charAt(correspFen.length()-1) != '8') {
                items.remove(items.get(i));
                i--;
            }
        }
        board.setItems(items);
        return board;
    }
}
