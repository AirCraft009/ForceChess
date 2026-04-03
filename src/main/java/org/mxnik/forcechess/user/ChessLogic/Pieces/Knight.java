package org.mxnik.forcechess.user.ChessLogic.Pieces;

import static org.mxnik.forcechess.user.ChessLogic.Moves.MoveOffsets.*;
import static org.mxnik.forcechess.Util.Helper.*;

public class Knight extends Piece{
    public final static int dirCount = 8;
    private static final byte[] moveSet = new byte[dirCount];
    static {
        refreshMoveSet();
    }

    static void refreshMoveSet() {
        moveSet[0] = (byte) (RIGHT.offset + UP.offset * 2);
        moveSet[1] = (byte) (LEFT.offset + UP.offset * 2);
        moveSet[2] = (byte) (RIGHT.offset + DOWN.offset * 2);
        moveSet[3] = (byte) (LEFT.offset + DOWN.offset * 2);
        moveSet[4] = (byte) (RIGHT.offset * 2 + UP.offset);
        moveSet[5] = (byte) (LEFT.offset * 2 + UP.offset);
        moveSet[6] = (byte) (RIGHT.offset * 2 + DOWN.offset);
        moveSet[7] = (byte) (LEFT.offset * 2 + DOWN.offset);
    }

    @Override
    public int getMaxDir() {
        return dirCount;
    }

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
