package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Board;

import java.lang.Math;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Bishop extends Piece {
    private final byte[] moveSet = new byte[(Board.sideLen - 1) * 2];
    public Bishop( boolean color, boolean hasMoved) {
        super(PieceTypes.BISHOP, color, hasMoved);
    }


    @Override
    byte[] getMoveSet() {
        return moveSet;
    }

    @Override
    public byte[] getMoves(int pos) {
        // code shaut hässlich aus ist aber nicht so uneffizient.
        // läuft immer noch O(N)

        byte[] finalMs = new byte[moveSet.length];
        int movePtr = 0;

        int dLeft = distanceLeftB(pos);
        int dTop = distanceTopB(pos);
        int dRight = distanceRightB(pos);
        int dBott = distanceBottomB(pos);

        // lönge der Diagonale ist das minimum zwischen den seiten

        for (int i = 1; i <= Math.min(dLeft, dTop); i++) {
            finalMs[movePtr] = (byte) (i * UP_L.offset);
            movePtr ++;
        }
        for (int i = 1; i <= (Math.min(dRight, dTop)); i++) {
            finalMs[movePtr] = (byte) (i * UP_R.offset);
            movePtr ++;
        }

        for (int i = 1; i <= Math.min(dBott, dRight); i++) {
            finalMs[movePtr] = (byte) (i * DOWN_R.offset);
            movePtr ++;
        }

        for (int i = 1; i <= Math.min(dBott, dLeft); i++) {
            finalMs[movePtr] = (byte) (i * DOWN_L.offset);
            movePtr ++;
        }
        return finalMs;
    }

    public boolean isValidMove(int from, int to){
        if(!isInside(to)) return false;

        return getRow(from) == getRow(to)
                || getCol(from) == getCol(to);
    }
}
