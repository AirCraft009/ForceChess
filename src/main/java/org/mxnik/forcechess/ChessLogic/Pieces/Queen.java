package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Board;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.DOWN;

public class Queen extends Piece{
    private final byte[] moveSet = new byte[(Board.sideLen - 1) * 4];

    public Queen(boolean color, boolean hasMoved) {
        super(PieceTypes.QUEEN, color, hasMoved);
    }

    @Override
    boolean isValidMove(int from, int to) {
        if(!isInside(to)) return false;

        int r = rowDiff(from, to);
        int c = colDiff(from, to);

        return r == c || r == 0 || c == 0;
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


        for (int i = 1; i <= dLeft; i++) {
            finalMs[movePtr] = (byte) (i * LEFT.offset);
            movePtr ++;
        }
        for (int i = 1; i <= (Board.sideLen - dLeft) - 1; i++) {
            finalMs[movePtr] = (byte) (i * RIGHT.offset);
            movePtr ++;
        }

        for (int i = 1; i <= dTop; i++) {
            finalMs[movePtr] = (byte) (i * UP.offset);
            movePtr ++;
        }

        for (int i = 1; i <= (Board.sideLen - dTop) - 1 ; i++) {
            finalMs[movePtr] = (byte) (i * DOWN.offset);
            movePtr ++;
        }

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
}
