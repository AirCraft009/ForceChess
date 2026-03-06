package org.mxnik.forcechess.ChesGame.UI.ChessControllView;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.Util.Helper;

import java.util.Arrays;


public class ChessController implements EventHandler<Event> {

    final private ChessScene chessScene;
    final private Board board;
    private byte[][] currentMoveState;
    private boolean turn = true;
    private int firstClick = -1;
    private int secondClick = -1;
    private boolean pieceSelected = false;
    private byte[] currPieceMoves;

    private int activeSquare = -1;
    private int lastClicked = 0;
    private boolean selected = false;

    public ChessController(ChessScene chess, String startFen){
        chessScene = chess;
        board = new Board(startFen, (byte) 2);
        chessScene.drawPieces(board.getBoard());
        currentMoveState = board.getMoveFromPosition();
        currPieceMoves = new byte[0];
    }

    public void handleActionEvent(ActionEvent event) throws CloneNotSupportedException {
        Object source = event.getSource();

        // all Buttons
        if (source instanceof ChessButton sourceButton){

            // handle menu buttons

            //handle field buttons
            //durchschnittlich 70 micros max 100 micros -> 0.0000999 sec
            int buttonField = sourceButton.getField();

            if(board.getBoard()[buttonField].getColor() != turn) {
                return;
            }

            chessScene.resetBoard();
            byte[] moves = currentMoveState[buttonField];

            boolean hasPiece = board.getBoard()[buttonField] != EmptyPiece.EMPTY_PIECE;


            if(!pieceSelected) {
                firstClick = buttonField;
                highlightSquares(moves);
            }else {
                secondClick = buttonField;
            }


            handleSquare(hasPiece);

            ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(buttonField);
            if(!pieceSelected){
                oldRect.deactivate();
            }else {
                oldRect.setActive();
            }
            currPieceMoves = moves;
            //System.out.println(pieceSelected);
            chessScene.drawPieces(board.getBoard());
            turn = !turn;
        }
    }

    public void highlightSquares(byte[] moves){
        for (byte move : moves){
            ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(move);
            oldRect.toggle();
        }
    }

    public void handleSquare(boolean hasPiece) throws CloneNotSupportedException {
        if (pieceSelected) {
            //condition: -> firstCLick != -1;
            pieceSelected = false;
            if (Helper.contains(currPieceMoves, secondClick)) {
                board.move(firstClick, secondClick);
                currentMoveState = board.getMoveFromPosition();
            }
            return;
        }
        if (hasPiece) {
            pieceSelected = true;
        }
    }

    @Override
    public void handle(Event event) {
        if (event instanceof ActionEvent){
            try {
                handleActionEvent((ActionEvent) event);
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
