package org.mxnik.forcechess.ChessLogic.Pieces;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class King extends Piece{
    private final byte[] moveSet = {
            UP.offset,
            RIGHT.offset,
            DOWN.offset,
            LEFT.offset
    };

    public King(boolean color, boolean hasMoved) {
        super(PieceTypes.KING, color, hasMoved);
    }

    public boolean isValidMove(int from, int to){
        if(!isInside(to)) return false;

        int r = rowDiff(from, to);
        int c = colDiff(from, to);

        return r <= 1 && c <= 1 && (r + c) != 0;
    }

    @Override
    byte[] getMoveSet() {
        return moveSet;
    }

}
