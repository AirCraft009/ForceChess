package org.mxnik.forcechess.ChessLogic.Pieces;

import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;
import static org.mxnik.forcechess.ChessLogic.Board.BoardHelper.*;

public class Knight extends Piece {
    public final static int dirCount = 8;
    private static final int[] moveSet = new int[dirCount];
    static {
        refreshMoveSet();
    }

    static void refreshMoveSet() {
        moveSet[0] = RIGHT.offset + UP.offset * 2;
        moveSet[1] = LEFT.offset + UP.offset * 2;
        moveSet[2] = RIGHT.offset + DOWN.offset * 2;
        moveSet[3] = LEFT.offset + DOWN.offset * 2;
        moveSet[4] = RIGHT.offset * 2 + UP.offset;
        moveSet[5] = LEFT.offset * 2 + UP.offset;
        moveSet[6] = RIGHT.offset * 2 + DOWN.offset;
        moveSet[7] = LEFT.offset * 2 + DOWN.offset;
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
    int[] getMoveSet() {
        return moveSet;
    }

    public Knight(boolean color, boolean hasMoved) {
        super(PieceTypes.KNIGHT, color, hasMoved);
    }
}
