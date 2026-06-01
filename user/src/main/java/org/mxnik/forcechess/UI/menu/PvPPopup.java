package org.mxnik.forcechess.UI.menu;

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
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.UI.ChessControllView.ChessView;

public class PvPPopup extends MenuPopup {
    /**
     * Creates a popup-window for starting a player vs player match
     * @param primaryStage the Stage the match will be displayed in
     */
    public PvPPopup(Stage primaryStage) {
        super(primaryStage);
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.setHgap(10);
        grid.setVgap(50);
        grid.setBackground(Background.fill(Color.LIGHTGRAY));

        Label boardText = new Label("Board: ");
        grid.add(boardText, 0, 0);

        ChoiceBox<String> board = boardCB(-1);
        board.getSelectionModel().select("default");
        grid.add(board, 1, 0);

        Button cancel = getButton("Cancel");
        cancel.setOnAction(e -> close());
        grid.add(cancel, 0, 1);

        Button contButton = getButton("Continue");
        contButton.setOnAction(e -> {
            close();
            String fen = FenProperties.getFenStr(board.getValue());
            int sideLen = Integer.parseInt(String.valueOf(fen.charAt(fen.length()-1)));
            try {
                new ChessView(primaryStage, fen, sideLen, null, null, 0);
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex);
            }
        });
        grid.add(contButton, 1, 1);

        Scene scene = new Scene(grid);
        setScene(scene);
        setTitle("PvP");
        show();
    }
}
