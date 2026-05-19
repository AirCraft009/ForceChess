package org.mxnik.forcechess.UI.menu;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.GridPane;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.UI.ChessControllView.ChessScene;

public class PvPPopup extends MenuPopup {
    public PvPPopup() {
        super();
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ChoiceBox<String> board = boardCB();
        grid.add(board, 0, 0);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> close());
        grid.add(cancel, 0, 1);

        Button contButton = new Button("Continue");
        contButton.setOnAction(e -> {
            close();
            String fen = FenProperties.getFenStr(board.getValue());
            int sideLen = Integer.parseInt(String.valueOf(fen.charAt(fen.length()-1)));
            try {
                new ChessScene(sideLen);
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

    private ChoiceBox<String> boardCB(){
        ChoiceBox<String> board = new ChoiceBox<>();
        FenProperties.load();
        ObservableList<String> items = FXCollections.observableArrayList(FenProperties.fenNames);
        board.setItems(items);
        return board;
    }
}
