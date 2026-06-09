package org.mxnik.forcechess.Chess;

import org.mxnik.forcechess.GameControl.Callback;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen;
import org.mxnik.forcechess.Moves.GameState;
import org.mxnik.forcechess.Moves.MovePacket;
import org.mxnik.forcechess.GameControl.Player;

import java.io.IOException;

public final class ChessGame implements Runnable{
    private  Player white;
    private  Player black;
    private boolean running;
    private final Board board;
    private final Callback response;
    private final Thread requestThread;

    private int lastMoveFrom = -1, lastMoveTo = -1;

    public ChessGame( Board board, Callback response){
        this.board = board;
        this.response = response;
        requestThread = new Thread(ChessGame.this);
    }

    public void setPlayers(Player white, Player black){
        this.black = black;
        this.white = white;
        System.out.println(white);
        System.out.println(black);
    }

    public void startGame(){
        running = true;
        requestThread.start();
    }

    public void resign(){
        response.finish(GameState.Resignation);
    }

    public void closePlayers() throws IOException {
        if(white == null){
            return;
        }

        if(white == black) {
            white.close();
            return;
        }

        white.close();
        black.close();
    }

    public void stop(){
        running = false;

    }

    public Player getActivePLayer(){
        return board.getTurn() ? white : black;
    }

    public void undoMove(){
        if(white == black) {
            white.undoMove();
            return;
        }

        white.undoMove();
        black.undoMove();
    }

    @Override
    public void run() {
        while (running) {
            try {
                var state = ChessMoveGen.getMovesFromPosition(board);
                if (state.second() != GameState.Continue) {
                    response.finish(state.second());
                    break;
                }
                MovePacket packet = getActivePLayer().requestMove();
                if(white == black)
                    white.makeMove(packet);
                else {
                    white.makeMove(packet);
                    black.makeMove(packet);
                }

                lastMoveFrom = packet.from();
                lastMoveTo = packet.to();

                board.move(packet);
                response.update();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getLastMoveFrom(){
        return lastMoveFrom;
    }
    public int getLastMoveTo(){
        return lastMoveTo;
    }
}