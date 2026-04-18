package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.MoveList;

import static org.mxnik.forcechess.ChessLogic.BoardHelper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class King extends Piece{
    public static final int dirCount = 10;
    private static final byte[] moveSet = new byte[dirCount];
    static {
        refreshMoveSet();
    }

    static void refreshMoveSet() {
        moveSet[0] = UP.offset;
        moveSet[1] = RIGHT.offset;
        moveSet[2] = DOWN.offset;
        moveSet[3] = LEFT.offset;
        moveSet[4] = UP_R.offset;
        moveSet[5] = UP_L.offset;
        moveSet[6] = DOWN_R.offset;
        moveSet[7] = DOWN_L.offset;
        moveSet[8] = (byte) (RIGHT.offset * 2);
        moveSet[9] = (byte) (LEFT.offset * 2);
    }

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

        return r <= 1 && c <= 2 && (r + c) != 0;
    }

    @Override
    byte[] getMoveSet() {
        return moveSet;
    }

    @Override
    public void getMoves(int pos, MoveList moveList){
        moveList.startPiece();
        byte[] mSet = this.getMoveSet();

        for (byte moveOffset : mSet) {
            int target = pos + moveOffset * ((color)? 1 : -1);
            if (!isValidMove(pos, target)) {
                continue;
            }

            moveList.startDirection();
            moveList.addMove((byte) target);
        }
    }
}
