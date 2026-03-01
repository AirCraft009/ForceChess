package org.mxnik.forcechess.UI;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.shape.Rectangle;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.Util.DiversePair;

import java.util.Arrays;


public class ChessController implements EventHandler<Event> {

    final private ChessScene chessScene;
    final private Board board;
    private DiversePair<byte[], Byte>[] currentMoveState;

    public ChessController(ChessScene chess, String startFen){
        chessScene = chess;
        board = new Board(startFen, (byte) 2);
        chessScene.drawPieces(board.getBoard(), Board.sideLen);
        currentMoveState = board.getMoveFromPosition();
    }

    public void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();

        // all Buttons
        if (source instanceof ChessButton sourceButton){

            // handle menu buttons

            //handle field buttons
            //durchschnittlich 70 micros max 100 micros -> 0.0000999 sec
            for (int i = 0; i < currentMoveState.length; i++) {
                if (sourceButton.getField() == currentMoveState[i].second()) {
//                    Rectangle rect = (Rectangle) chessScene.backgroundLayer.getChildren().get(i);
//                    rect.setStyle("-fx-border-style: solid; -fx-border-width: 5; -fx-border-color: black;");
                    System.out.println(Arrays.toString(currentMoveState[i].first()));
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
