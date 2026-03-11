package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.MoveList;

import static org.mxnik.forcechess.Util.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class King extends Piece{
    public static final int dirCount = 10;
    private final byte[] moveSet = {
            UP.offset,
            RIGHT.offset,
            DOWN.offset,
            LEFT.offset,
            UP_R.offset,
            UP_L.offset,
            DOWN_R.offset,
            DOWN_L.offset,
            (byte) (RIGHT.offset*2),
            (byte) (LEFT.offset*2)
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
