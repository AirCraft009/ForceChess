package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Board;

import java.util.ArrayList;
import java.util.List;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Rook extends Piece {
    private static final byte[] EMPTY_DIRECTION = new byte[0];
    private final byte[][] moveSet = {EMPTY_DIRECTION, EMPTY_DIRECTION, EMPTY_DIRECTION, EMPTY_DIRECTION};

    public Rook(boolean color, boolean hasMoved) {
        super(PieceTypes.ROOK, color, hasMoved);
    }


    @Override
    public byte[][] getMoveSet() {
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
    public byte[][] getMoves(int pos) {
        List<byte[]> directions = new ArrayList<>();

        int dLeft = distanceLeftB(pos);
        int dTop = distanceTopB(pos);

        if (dLeft > 0) {
            byte[] leftMoves = new byte[dLeft];
            for (int i = 1; i <= dLeft; i++) {
                leftMoves[i - 1] = (byte) (pos + i * LEFT.offset);
            }
            directions.add(leftMoves);
        }

        int dRight = (Board.sideLen - dLeft) - 1;
        if (dRight > 0) {
            byte[] rightMoves = new byte[dRight];
            for (int i = 1; i <= dRight; i++) {
                rightMoves[i - 1] = (byte) (pos + i * RIGHT.offset);
            }
            directions.add(rightMoves);
        }

        if (dTop > 0) {
            byte[] upMoves = new byte[dTop];
            for (int i = 1; i <= dTop; i++) {
                upMoves[i - 1] = (byte) (pos + i * UP.offset);
            }
            directions.add(upMoves);
        }

        int dBottom = (Board.sideLen - dTop) - 1;
        if (dBottom > 0) {
            byte[] downMoves = new byte[dBottom];
            for (int i = 1; i <= dBottom; i++) {
                downMoves[i - 1] = (byte) (pos + i * DOWN.offset);
            }
            directions.add(downMoves);
        }

        return directions.toArray(new byte[0][]);
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
