package org.mxnik.forcechess.Chess;

import org.mxnik.forcechess.Callback;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen;
import org.mxnik.forcechess.GameState;
import org.mxnik.forcechess.MovePacket;
import org.mxnik.forcechess.Player;

public final class ChessGame implements Runnable{
    private final Player white;
    private final Player black;
    private final Board board;
    private final Callback response;
    private Thread requestThread;

    public ChessGame(Player white, Player black, Board board, Callback response){
        this.white = white;
        this.black = white;
        this.board = board;
        this.response = response;
        requestThread = new Thread(ChessGame.this);
    }

    public void startGame(){
        requestThread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                var state = ChessMoveGen.getMovesFromPosition(board);
                if (state.second() != GameState.Continue) {
                    response.finish();
                    break;
                }
                MovePacket packet = board.getTurn() ? white.requestMove(state.first()) : black.requestMove(state.first());
                //TODO: implemented MovePacket inner workings and connect them with board
                board.move(0,0);
                response.update();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
