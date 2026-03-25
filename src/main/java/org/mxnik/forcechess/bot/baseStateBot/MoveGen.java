package org.mxnik.forcechess.bot.baseStateBot;

public class MoveGen {
    /**
     * Generates all possible moves for the side to move
     * @param position current gamestate
     * @param offset start in the move array
     * @param moves all moves from top of MCTS to bottom depth max 256 * max search depth
     * @return new offset
     */
    public int generateMoves (PositionEncoder.Position position, int offset, int[] moves){
        // WhitePawns

        long singleP = (position.WPawns.board << PositionEncoder.SIZE) & position.Occupied.board;
        long doubleP = (position.WPawns.board << PositionEncoder.SIZE * 2) & position.WDoublePawnMove.board & position.Occupied.board;
        long attackL = (position.WPawns.board << PositionEncoder.SIZE) & position.Occupied.board;
        return offset;
    }
}
