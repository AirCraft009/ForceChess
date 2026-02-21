package org.mxnik.forcechess.ChessLogic.Pieces;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.Helper.getCol;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.UP;

public class Bishop extends Piece {
    private final byte[] moveSet = {
            (byte) (UP.offset * 2),
            UP.offset
    };
    public Bishop( boolean color, boolean hasMoved) {
        super(PieceTypes.BISHOP, color, hasMoved);
    }


    @Override
    byte[] getMoveSet() {
        return new byte[0];
    }

    public boolean isValidMove(int from, int to){
        if(!isInside(to)) return false;

        return getRow(from) == getRow(to)
                || getCol(from) == getCol(to);
    }
}
