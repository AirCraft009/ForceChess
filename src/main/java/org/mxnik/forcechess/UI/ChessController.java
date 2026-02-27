package org.mxnik.forcechess.UI;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.Util.DiversePair;

import java.util.Arrays;


public class ChessController implements EventHandler<Event> {

    final private ChessScene chessScene;
    final private Board board;

    public ChessController(ChessScene chess, String startFen){
        chessScene = chess;
        board = new Board(startFen, (byte) 2);
        chessScene.drawPieces(board.getBoard(), Board.sideLen);

    }

    public void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();

        // all Buttons
        if (source instanceof ChessButton sourceButton){

            // handle menu buttons

            //handle field buttons
            DiversePair<byte[], Byte>[] diversePair = board.getMoveFromPosition();
            for (int i = 0; i < diversePair.length; i++) {
                if (sourceButton.getField() == diversePair[i].second()) {
                    System.out.println(Arrays.toString(diversePair[i].first()));
                }
            }

        }
    }

    @Override
    public void handle(Event event) {
        if (event instanceof ActionEvent){
            handleActionEvent((ActionEvent) event);
        }
    }
}
