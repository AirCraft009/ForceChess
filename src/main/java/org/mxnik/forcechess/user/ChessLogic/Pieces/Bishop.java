package org.mxnik.forcechess.user.ChessLogic.Pieces;

import org.mxnik.forcechess.user.ChessLogic.Board;
import org.mxnik.forcechess.user.ChessLogic.Moves.MoveList;
import static org.mxnik.forcechess.Util.Helper.*;

import java.lang.Math;

import static org.mxnik.forcechess.user.ChessLogic.Moves.MoveOffsets.*;

public class Bishop extends Piece {
    public final static int dirCount = 4;
    private static byte[] moveSet = new byte[(Board.sideLen - 1) * 2];
    static {
        refreshMoveSet();
    }

    static void refreshMoveSet() {
        int size = (Board.sideLen - 1) * 2;
        if (moveSet.length != size) {
            moveSet = new byte[size];
        }
    }

    public Bishop( boolean color, boolean hasMoved) {
        super(PieceTypes.BISHOP, color, hasMoved);
    }


    @Override
    public int getMaxDir() {
        return dirCount;
    }

    @Override
    byte[] getMoveSet() {
        return moveSet;
    }

    @Override
    public void getMoves(int pos, MoveList moveList) {
        // code shaut hässlich aus ist aber nicht so uneffizient.
        // läuft immer noch O(N)
        moveList.startPiece();

        int dLeft = distanceLeftB(pos);
        int dTop = distanceTopB(pos);
        int dRight = distanceRightB(pos);
        int dBott = distanceBottomB(pos);

        // lönge der Diagonale ist das minimum zwischen den seiten

        moveList.startDirection();
        for (int i = 1; i <= Math.min(dLeft, dTop); i++) {
            moveList.addMove((byte) (pos + i * UP_L.offset));
        }

        moveList.startDirection();
        for (int i = 1; i <= (Math.min(dRight, dTop)); i++) {
            moveList.addMove((byte) (pos + i * UP_R.offset));
        }

        moveList.startDirection();
        for (int i = 1; i <= Math.min(dBott, dRight); i++) {
            moveList.addMove((byte) (pos + i * DOWN_R.offset));
        }

        moveList.startDirection();
        for (int i = 1; i <= Math.min(dBott, dLeft); i++) {
            moveList.addMove((byte) (pos + i * DOWN_L.offset));
        }
    }

    public boolean isValidMove(int from, int to){
        if(!isInside(to)) return false;

        return getRow(from) == getRow(to)
                || getCol(from) == getCol(to);
    }
}
