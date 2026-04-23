package org.mxnik.forcechess.UI.ChessControllView;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import org.mxnik.forcechess.*;
import org.mxnik.forcechess.Chess.ChessGame;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen;
import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.ChessLogic.Board.BoardHelper;

import java.io.IOException;

import static org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen.getMovesFromPosition;


public class ChessController implements EventHandler<Event>, Callback, Player {

    final private ChessScene chessScene;
    final private Board board;
    final private ChessGame game;
    private DiversePair<byte[][], GameState> currentMoveState;
    private boolean turn = true;
    private int firstClick = -1;
    private int secondClick = -1;
    private boolean pieceSelected = false;
    private byte[] currPieceMoves;
    private volatile boolean moveReady;


    public ChessController(ChessScene chess, String startFen) throws CloneNotSupportedException, IOException {
        chessScene = chess;
        moveReady = true;
        board = new Board(startFen);
        game = new ChessGame(board, this);
        chessScene.drawPieces(board.getBoard());
        currentMoveState = getMovesFromPosition(board);
        currPieceMoves = new byte[0];
    }

    public void setPlayers(Player white, Player black){
        game.setPlayers(white,black);
    }

    public void start(){
        game.run();
    }

    public void handleActiveChessClick(ChessButton sourceButton) throws CloneNotSupportedException {
        // handle menu buttons

        //handle field buttons
        //durchschnittlich 70 micros max 100 micros -> 0.0000999 sec

        if(game.getActivePLayer() != this){
            // only do with the right players
            return;
        }

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

    }

    public void handleActionEvent(ActionEvent event) throws CloneNotSupportedException {
        Object source = event.getSource();

        // all Buttons
        if (source instanceof ChessButton sourceButton){
            handleActiveChessClick(sourceButton);
        }
    }

    public void resetToDefault(){
        firstClick = -1;
        secondClick = -1;
        pieceSelected = false;
    }



    /**
     * set all squares held in the move array to active
     * This also sets the color to the secondary
     * @param moves arr of fields (max Board.size -1)
     */
    public void highlightSquares(byte[] moves){
        //
        for (byte move : moves) {
            ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(move);
            oldRect.setActive();
        }
    }

    /**
     * handle the logic behind highlighting and moving Pieces
     * @param hasPiece does the square contain a piece
     */
    public void handleSquare(boolean hasPiece) throws CloneNotSupportedException {
        if (pieceSelected) {
            //condition: -> firstCLick != -1;
            pieceSelected = false;
            if (BoardHelper.contains(currPieceMoves, secondClick)) {
                moveReady = true;
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

    @Override
    public void update() {
        chessScene.drawPieces(board.getBoard());
    }

    @Override
    public void finish() {
        //TODO: show the win/loose screen and end game
    }

    @Override
    public MovePacket requestMove(byte[][] possibleMoves) {
        while (!moveReady) {
            Thread.onSpinWait();
            //poll for move ready
        }
        System.out.println("move issued");
        return new MovePacket(MoveType.Generic, firstClick, secondClick, false);
    }
}
