package org.mxnik.forcechess.ChessLogic.Pieces;
import org.mxnik.forcechess.ChessLogic.Moves.Helper;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Knight extends Piece{
    private final byte[] moveSet = {
            (byte) (RIGHT.offset + UP.offset * 2),
            (byte) (LEFT.offset + UP.offset * 2),
            (byte) (RIGHT.offset + DOWN.offset * 2),
            (byte) (LEFT.offset + DOWN.offset * 2),

            (byte) (RIGHT.offset * 2 + UP.offset),
            (byte) (LEFT.offset * 2 + UP.offset),
            (byte) (RIGHT.offset * 2 + DOWN.offset),
            (byte) (LEFT.offset * 2+ DOWN.offset),
    };

    @Override
    public boolean isValidMove(int from, int to){
        if(!isInside(to)) return false;

        int r = rowDiff(from, to);
        int c = colDiff(from, to);

        return (r == 2 && c == 1) || (r == 1 && c == 2);
    }

    @Override
    byte[] getMoveSet() {
        return moveSet;
    }

    public Knight(boolean color, boolean hasMoved) {
        super(PieceTypes.KNIGHT, color, hasMoved);
    }
}
