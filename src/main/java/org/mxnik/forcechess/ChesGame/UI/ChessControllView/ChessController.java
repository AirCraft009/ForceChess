package org.mxnik.forcechess.ChesGame.UI.ChessControllView;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.GameState;
import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.Util.Helper;


public class ChessController implements EventHandler<Event> {

    final private ChessScene chessScene;
    final private Board board;
    private DiversePair<byte[][], GameState> currentMoveState;
    private boolean turn = true;
    private int firstClick = -1;
    private int secondClick = -1;
    private boolean pieceSelected = false;
    private byte[] currPieceMoves;


    public ChessController(ChessScene chess, String startFen) throws CloneNotSupportedException {
        chessScene = chess;
        board = new Board(startFen);
        chessScene.drawPieces(board.getBoard());
        currentMoveState = board.getMovesFromPosition();
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


            byte[] moves = currentMoveState.first()[buttonField];

            boolean hasPiece = board.getBoard()[buttonField] != EmptyPiece.EMPTY_PIECE;
            boolean pieceColor = board.getBoard()[buttonField].getColor();
            if(pieceColor != turn && hasPiece && !pieceSelected) {
                return;
            }

            chessScene.resetBoard();
            if(!pieceSelected) {
                firstClick = buttonField;
                highlightSquares(moves);
            }else {
                if(pieceColor == turn && hasPiece && buttonField != firstClick) {
                    firstClick = buttonField;
                    highlightSquares(moves);
                    pieceSelected = false;
                }else {
                    secondClick = buttonField;
                }
            }


            handleSquare(hasPiece);

            ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(buttonField);
            if(!pieceSelected){
                oldRect.deactivate();
            }else {
                oldRect.setActive();
            }
            currPieceMoves = moves;
            chessScene.drawPieces(board.getBoard());
        }
    }

    /**
     * set all squares held in the move array to active
     * This also sets the color to the secondary
     * @param moves arr of fields (max Board.size -1)
     */
    public void highlightSquares(byte[] moves){
        for (byte move : moves){
            ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(move);
            oldRect.setActive();
        }
    }

    /**
     * handle the logic behind highlighting and moving Pieces
     * @param hasPiece does the square contain a piece
     * @throws CloneNotSupportedException
     */
    public void handleSquare(boolean hasPiece) throws CloneNotSupportedException {
        if (pieceSelected) {
            //condition: -> firstCLick != -1;
            pieceSelected = false;
            if (Helper.contains(currPieceMoves, secondClick)) {
                board.move(firstClick, secondClick);
                currentMoveState = board.getMovesFromPosition();
                if (currentMoveState.second() != GameState.Continue){
                    chessScene.showWinImage();
                }
                turn = !turn;
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
