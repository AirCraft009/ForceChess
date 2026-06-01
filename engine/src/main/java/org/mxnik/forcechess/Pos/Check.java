package org.mxnik.forcechess.Pos;

import static org.mxnik.forcechess.Pos.MoveGen.generatePseudoMoves;
import static org.mxnik.forcechess.bot.ChessBot.MAX_MOVES_IN_POS;

public class Check {
    private static int[] moves = new int[MAX_MOVES_IN_POS];

    /**
     * Utility to see if a position has moves (useful for checking for stale/check-mate
     */
    public static boolean hasMoves(PositionEncoder.Position pos, boolean whiteToMove) {
        int pseudoOffset = generatePseudoMoves(pos, 0, whiteToMove, moves);

        for (int i = 0; i < pseudoOffset; i++) {
            int undo = pos.makeMove(moves[i]);

            if (pos.checkChess(whiteToMove)) {
                pos.unmakeMove(undo);
                continue;                                   // don't add move
            }

            // move exists
            pos.unmakeMove(undo);
            return true;
        }
        return false;
    }
}
