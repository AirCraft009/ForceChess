package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Moves.MoveList;

import static org.mxnik.forcechess.Util.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.DOWN;

public class Queen extends Piece{
    public final static int dirCount = 8;
    private static byte[] moveSet = new byte[(Board.sideLen - 1) * 4];
    static {
        refreshMoveSet();
    }

    static void refreshMoveSet() {
        int size = (Board.sideLen - 1) * 4;
        if (moveSet.length != size) {
            moveSet = new byte[size];
        }
    }

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


        moveList.startDirection();
        for (int i = 1; i <= dLeft; i++) {
            moveList.addMove((byte) (pos + i * LEFT.offset));
        }

        moveList.startDirection();
        for (int i = 1; i <= (Board.sideLen - dLeft) - 1; i++) {
            moveList.addMove((byte) (pos + i * RIGHT.offset));
        }

        moveList.startDirection();
        for (int i = 1; i <= dTop; i++) {
            moveList.addMove((byte) (pos + i * UP.offset));
        }

        moveList.startDirection();
        for (int i = 1; i <= (Board.sideLen - dTop) - 1 ; i++) {
            moveList.addMove((byte) (pos + i * DOWN.offset));
        }

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
}
