package org.mxnik.forcechess.ChessLogic.Pieces;

import static org.mxnik.forcechess.Util.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class King extends Piece{
    public static final int dirCount = 8;
    private final byte[] moveSet = {
            UP.offset,
            RIGHT.offset,
            DOWN.offset,
            LEFT.offset,
            UP_R.offset,
            UP_L.offset,
            DOWN_R.offset,
            DOWN_L.offset
    };

    @Override
    public int getMaxDir() {
        return dirCount;
    }

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
