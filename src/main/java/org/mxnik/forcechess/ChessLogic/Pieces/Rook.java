package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Board;

import java.lang.reflect.Array;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.Helper.getCol;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Rook extends Piece {
    // 7 (boardlen -1) in each cardinal direction
    private final byte[] moveSet = new byte[(Board.sideLen - 1) * 2];
    public Rook( boolean color, boolean hasMoved) {
        super(PieceTypes.ROOK, color, hasMoved);
    }


    @Override
    public byte[] getMoveSet() {
        return moveSet;
    }


    /**
     * get Moves für den Turm.
     * Gibt alle möglichen Züge zurück, ohne ander Schachfiguren zu beachten.
     * Moves werden nur durch die Boardgrenzen eingeschränkt
     * @param pos die jetzige position des Turms
     * @return ein byte arr mit allen move offsets
     */
    @Override
    public byte[] getMoves(int pos) {
        // code shaut hässlich aus ist aber nicht so uneffizient.
        // läuft immer noch O(N)

        byte[] finalMs = new byte[moveSet.length];
        int movePtr = 0;

        int dLeft = distanceLeftB(pos);
        int dTop = distanceTopB(pos);
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
        return finalMs;
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
