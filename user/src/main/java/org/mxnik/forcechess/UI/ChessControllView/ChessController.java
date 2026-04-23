package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Platform;
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
    private int firstClick = -1;
    private int secondClick = -1;
    private boolean pieceSelected = false;
    private byte[] currPieceMoves;
    private volatile boolean moveReady;


    public ChessController(ChessScene chess, String startFen) throws CloneNotSupportedException, IOException {
        chessScene = chess;
        moveReady = false;
        board = new Board(startFen);
        game = new ChessGame(board, this);
        chessScene.drawPieces(board.getBoard());
        currentMoveState = getMovesFromPosition(board);
        currPieceMoves = new byte[0];
    }

    /**
     * sets the players that will take turns
     */
    public void setPlayers(Player white, Player black){
        game.setPlayers(white,black);
    }

    /**
     * starts the thread for the game
     */
    public void start(){
        game.startGame();
    }

    /**
     * stops the thread in the game so it doesn't become a zombie process
     */
    public void cleanUp(){
        game.stop();
    }

    /**
     * should a click be handled by the method checks if there is a piece the current player is the controller and the color is right
     */
    public boolean shouldHandle(int field, boolean hasPiece, boolean pieceColor){
        //handle field buttons
        //durchschnittlich 70 micros max 100 micros -> 0.0000999 sec

        if(game.getActivePLayer() != this){
            // only do with the right players
            return false;
        }

        return pieceColor == board.getTurn() || !hasPiece || pieceSelected;
    }

    /**
     * handles clicks on fields
     * @param sourceButton specific button (field) clicked
     */
    public void handleActiveChessClick(ChessButton sourceButton) throws CloneNotSupportedException {
        int buttonField = sourceButton.getField();
        boolean hasPiece = board.getBoard()[buttonField] != EmptyPiece.EMPTY_PIECE;
        boolean pieceColor = board.getBoard()[buttonField].getColor();

        if(!shouldHandle(buttonField, hasPiece, pieceColor)){
            return;
        }

        byte[] moves = currentMoveState.first()[buttonField];

        chessScene.resetBoard();        // clear pieces and highlights before setting them again

        if(!pieceSelected) {
            firstClick = buttonField;
            highlightSquares(moves);
        }else {
            secondClick = buttonField;
        }


        handleSquare(hasPiece);

        ChessBackgroundPane oldRect = (ChessBackgroundPane) chessScene.backgroundLayer.getChildren().get(buttonField);
        if(pieceSelected){
            oldRect.setActive();
        }
        currPieceMoves = moves;

        chessScene.drawPieces(board.getBoard());
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
     * handles if to set the flag for moveReady
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
        try {
            currentMoveState = ChessMoveGen.getMovesFromPosition(board);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        Platform.runLater(() -> {
            chessScene.drawPieces(board.getBoard());
        });
    }

    @Override
    public void finish(GameState g) {
        //TODO: show the win/loose screen and end game
    }

    @Override
    public MovePacket requestMove() {
        while (!moveReady) {
            Thread.onSpinWait();
            //poll for move ready
        }
        //System.out.println("move issued");
        moveReady = false;
        return new MovePacket(MoveType.Generic, firstClick, secondClick, false);
    }

    @Override
    public MovePacket getMove() {
        return null;
    }
}
