package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Moves.MoveList;

import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;
import static org.mxnik.forcechess.ChessLogic.Board.BoardHelper.*;

public class Rook extends Piece {
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

    public Rook( boolean color, boolean hasMoved) {
        super(PieceTypes.ROOK, color, hasMoved);
    }


    @Override
    public byte[] getMoveSet() {
        return moveSet;
    }

    @Override
    public int getMaxDir() {
        return dirCount;
    }


    /**
     * get Moves für den Turm.
     * Gibt alle möglichen Züge zurück, ohne ander Schachfiguren zu beachten.
     * Moves werden nur durch die Boardgrenzen eingeschränkt
     * @param pos die jetzige position des Turms
     * @return ein byte arr mit allen move offsets
     */
    @Override
    public void getMoves(int pos, MoveList moveList) {
        // code shaut hässlich aus ist aber nicht so uneffizient.
        // läuft immer noch O(N)
        moveList.startPiece();

        int dLeft = distanceLeftB(pos);
        int dTop = distanceTopB(pos);

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
    }


    /**
     * is der Zug valide für einen Turm
     * @param from startpos
     * @param to endpos
     * @return ob der Zug sich in derselben Reihe oder Spalte aufenthält
     */
    public boolean isValidMove(int from, int to){
        if(!isInside(to)) return false;

        return getRow(from) == getRow(to)
                || getCol(from) == getCol(to);
    }
}
