package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.mxnik.forcechess.Chess.ChessGame;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen;
import org.mxnik.forcechess.ChessLogic.Moves.UndoMovePacket;
import org.mxnik.forcechess.ChessLogic.Notation.FenWriter;
import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.ChessLogic.Board.BoardHelper;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.GameControl.Callback;
import org.mxnik.forcechess.GameControl.Player;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Moves.GameState;
import org.mxnik.forcechess.Moves.MovePacket;
import org.mxnik.forcechess.Moves.MoveType;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.menu.MenuScene;

import java.io.IOException;
import java.util.Stack;
import java.util.concurrent.SynchronousQueue;

import static org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen.getMovesFromPosition;


public class ChessController implements EventHandler<Event>, Callback, Player {
    final private Stage stage;

    final private ChessView chessView;
    final private Board board;
    final private ChessGame game;
    private DiversePair<byte[][], GameState> currentMoveState;
    private int firstClick = -1;
    private int secondClick = -1;
    private boolean pieceSelected = false;
    private byte[] currPieceMoves;
    private final SynchronousQueue<MovePacket> moveQueue = new SynchronousQueue<>();
    private Stack<UndoMovePacket> undoStack = new Stack<>();


    int prevMovedFrom = -1, prevMovedTo = -1;


    public ChessController(ChessView chess, Stage stage, String startFen) throws CloneNotSupportedException, IOException {
        this.stage = stage;
        chessView = chess;
        board = new Board(startFen);
        game = new ChessGame(board, this);
        chessView.drawPieces(board);
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

    public void handlePromotionPress(int i, Stage stage){
        MoveType type = switch (i){
            case 0 -> MoveType.PromotionN;
            case 1 -> MoveType.PromotionB;
            case 2 -> MoveType.PromotionR;
            case 3 -> MoveType.PromotionQ;
            default -> null;
        };
        if(type == null)
            return;
        stage.close();
        moveQueue.offer(new MovePacket(type, firstClick, secondClick, board.getBoard()[secondClick] != EmptyPiece.EMPTY_PIECE));
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
    public void handleActiveChessClick(ChessButton sourceButton){
        int buttonField = sourceButton.getField();
        Piece[] cBoard = board.getBoard();
        boolean hasPiece = cBoard[buttonField] != EmptyPiece.EMPTY_PIECE;
        boolean pieceColor = cBoard[buttonField].getColor();

        if(!shouldHandle(buttonField, hasPiece, pieceColor)){
            return;
        }

        byte[] moves = currentMoveState.first()[buttonField];

        chessView.clearHighlights();        // clear pieces and highlights before setting them again

        if(!pieceSelected) {
            firstClick = buttonField;
            highlightSquares(moves);
        }else {
            secondClick = buttonField;
        }


        var packet = handleSquare(hasPiece);

        ChessBackgroundPane oldRect = (ChessBackgroundPane) chessView.backgroundLayer.getChildren().get(buttonField);
        if(pieceSelected){
            oldRect.setActive();
        }
        currPieceMoves = moves;

        if(packet != null) {
            moveQueue.offer(packet);
        }
    }

    public void handleActionEvent(ActionEvent event){
        Object source = event.getSource();

        // all Buttons
        if (source instanceof ChessButton sourceButton){
            handleActiveChessClick(sourceButton);
        }else if (source == chessView.saveB){
            FenProperties.addFenStr("current", FenWriter.WriteFen(board));
        }else if (source == chessView.quitB){
            cleanUp();
            new MenuScene(stage);
        } else if (source == chessView.undoB) {
            game.undoMove();
            update();
        } else if (source == chessView.resignB){
            //TODO Resign
        } else if (source == chessView.exit){
            cleanUp();
            new MenuScene(stage);
        } else if (source == chessView.newGame){
            try {
                cleanUp();
                // switch the colors
                new ChessView(new ChessView.ChessData(
                        chessView.currentGame.primaryStage(),
                        chessView.currentGame.fen(),
                        chessView.currentGame.playerStrB(),
                        chessView.currentGame.playerStrW(),
                        chessView.currentGame.playDepth()
                ));
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException("Clone cannot work one time, and break the second time");
            }
        }
    }

    public void handleKeyEvent(KeyEvent event) {
        Object source = event.getSource();
        if(event.getEventType() != KeyEvent.KEY_PRESSED){
            return;
        }

        switch (event.getCode()){
            case F11 -> stage.setFullScreen(!stage.isFullScreen());
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
            ChessBackgroundPane oldRect = (ChessBackgroundPane) chessView.backgroundLayer.getChildren().get(move);
            oldRect.setActive();
        }
    }

    public void highlightLastMove(){
        int lastMoveFrom = game.getLastMoveFrom();
        int lastMoveTo = game.getLastMoveTo();
        if(prevMovedFrom >= 0 && prevMovedTo >= 0) {
            ((ChessBackgroundPane) chessView.backgroundLayer.getChildren().get(prevMovedFrom)).deactivateMoved();
            ((ChessBackgroundPane) chessView.backgroundLayer.getChildren().get(prevMovedTo)).deactivateMoved();
        }
        ((ChessBackgroundPane) chessView.backgroundLayer.getChildren().get(lastMoveFrom)).setMoved();
        ((ChessBackgroundPane) chessView.backgroundLayer.getChildren().get(lastMoveTo)).setMoved();
        prevMovedFrom = lastMoveFrom;
        prevMovedTo = lastMoveTo;
    }

    /**
     * handles if to set the flag for moveReady
     * @param hasPiece does the square contain a piece
     */
    public MovePacket handleSquare(boolean hasPiece){
        if (!pieceSelected) {
            if (hasPiece) {
                pieceSelected = true;
            }
            return null;
        }

        pieceSelected = false;
        if (!BoardHelper.contains(currPieceMoves, secondClick)) {
            return null;
        }
        if(board.getBoard()[firstClick].getType() == PieceTypes.PAWN &&
            secondClick == board.getEnPassantPos()
        ){
            return new MovePacket(MoveType.EnPassant, firstClick, secondClick, true);       // enPassant is always a capture move
        }

        if( board.getBoard()[firstClick].getType() == PieceTypes.KING
            && (BoardHelper.colDiff(firstClick, secondClick) >= 2)
        ){
            return new MovePacket((firstClick - secondClick > 0)? MoveType.CastleQ : MoveType.CastleK, firstClick, secondClick, false);
        }
        if(  // check if the move is a promotion
            !(board.getBoard()[firstClick].getType() == PieceTypes.PAWN
            && (BoardHelper.getRow(secondClick) == 0
            || BoardHelper.getRow(secondClick) == Board.sideLen-1))
        ){
            // return normal move if NOT
            return new MovePacket(MoveType.Generic, firstClick, secondClick, board.getBoard()[secondClick] != EmptyPiece.EMPTY_PIECE);
        }

        double clickedX = BoardHelper.getCol(secondClick) * chessView.constants.BlockS + (double) chessView.constants.BlockS /2;
        double clickedY = (Board.sideLen-BoardHelper.getRow(secondClick)) * chessView.constants.BlockS - (double) chessView.constants.BlockS /2;

        double sceneY = chessView.stage.getY() + (chessView.stage.getHeight() - chessView.stage.getScene().getHeight());

        double x = chessView.stage.getX() + clickedX + chessView.constants.WidthStart;
        double y = sceneY + clickedY + chessView.constants.HeightStart;

        chessView.showPromotionStage(board.getBoard()[firstClick].getColor(), x, y);
        return null;
    }

    @Override
    public void handle(Event event) {
        if (event instanceof ActionEvent){
            handleActionEvent((ActionEvent) event);
        }else if(event instanceof KeyEvent){
            handleKeyEvent((KeyEvent) event);

        }
    }

    @Override
    public void update() {
        try {
            currentMoveState = ChessMoveGen.getMovesFromPosition(board);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.gc();

        Platform.runLater(() -> {
            chessView.drawPieces(board);
            highlightLastMove();
        });
    }

    @Override
    public void finish(GameState g) {
        Platform.runLater(() -> chessView.showWinImage(g, board));
    }

    @Override
    public MovePacket requestMove() {
        System.out.println("player move requested");
        try{
             return moveQueue.take();
        } catch (InterruptedException e) {
            System.err.println("Errored while getting move");
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void makeMove(MovePacket packet) throws CloneNotSupportedException {
        //don't do anything update handles it
        undoStack.push(
                new UndoMovePacket(packet, board.getBoard()[packet.from()].clone(),
                        board.getBoard()[packet.to()] = (board.getBoard()[packet.to()] == EmptyPiece.EMPTY_PIECE? EmptyPiece.EMPTY_PIECE : board.getBoard()[packet.to()].clone()),
                        board.getEnPassantPos(),
                        board.getFiftyMove(),
                        board.getCastleRights().clone()
                )
        );
    }

    @Override
    public void undoMove() {
        if(undoStack.isEmpty())
            return;
        board.undoMove(undoStack.pop());
    }

    /**
     * scale the viewed items properly
     */
    public void resize() {
        chessView.constants = new Constants(chessView.constants.sideLen, stage.getScene());
        chessView.backgroundLayer.getChildren().clear();
        chessView.menuLayer.getChildren().clear();
        chessView.clearInteractionLayer();
        chessView.drawBoard();
        chessView.drawMenuLayer();
        chessView.drawPieces(board);
    }
}
