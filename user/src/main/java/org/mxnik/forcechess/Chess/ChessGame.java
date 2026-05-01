package org.mxnik.forcechess.Chess;

import org.mxnik.forcechess.Callback;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen;
import org.mxnik.forcechess.GameState;
import org.mxnik.forcechess.MovePacket;
import org.mxnik.forcechess.Player;
import org.mxnik.forcechess.Pos.MoveGen;

public final class ChessGame implements Runnable{
    private  Player white;
    private  Player black;
    private boolean running;
    private final Board board;
    private final Callback response;
    private final Thread requestThread;

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

    public void stop(){
        running = false;
    }

    public Player getActivePLayer(){
        return board.getTurn() ? white : black;
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
                //TODO: implemented MovePacket inner workings and connect them with board
                board.move(packet.from(), packet.to());
                getActivePLayer().getMove(packet);
                //System.out.println("moved");
                response.update();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
