package org.mxnik.forcechess.ChesGame.UI.ChessControllView;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.Util.DiversePair;

import java.util.Arrays;


public class ChessController implements EventHandler<Event> {

    final private ChessScene chessScene;
    final private Board board;
    private final DiversePair<byte[], Byte>[] currentMoveState;
    private int activeSquare = -1;

    public ChessController(ChessScene chess, String startFen){
        chessScene = chess;
        board = new Board(startFen, (byte) 2);
        chessScene.drawPieces(board.getBoard());
        currentMoveState = board.getMoveFromPosition();
    }

    public void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();

        // all Buttons
        if (source instanceof ChessButton sourceButton){

            // handle menu buttons

            //handle field buttons
            //durchschnittlich 70 micros max 100 micros -> 0.0000999 sec
            int buttonfield = sourceButton.getField();
            int field = 0;
            boolean hasPiece = false;

            for (int i = 0; i < currentMoveState.length; i++) {
                field = currentMoveState[i].second();
                if (buttonfield == field) {
                    hasPiece = true;
                    highlightSquares(currentMoveState[i].first());
                    System.out.println(Arrays.toString(currentMoveState[i].first()));
                    break;
                }
                field = buttonfield;
            }
            handleSquareClick(field, hasPiece);
        }
    }

    public void highlightSquares(byte[] moves){
        for (byte move : moves){
            ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(move);
            oldRect.toggle();
        }
    }

    public void handleSquareClick(int field, boolean pieceField){
        if(activeSquare == -1) {
            activeSquare = field;
        }
        ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(activeSquare);

        if (!pieceField) {
            oldRect.deactivate();
            return;
        }

        ChessBackgroundPane newRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(field);
        if (activeSquare == field) {
            newRect.toggle();
            activeSquare = (newRect.isActive())? field: -1;
            return;
        }

        oldRect.deactivate();
        newRect.setActive();
        activeSquare = field;
    }

    @Override
    public void handle(Event event) {
        if (event instanceof ActionEvent){
            handleActionEvent((ActionEvent) event);
        }
    }
}
