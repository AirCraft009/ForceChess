package org.mxnik.forcechess.ChessLogic.Pieces;

import java.util.ArrayList;
import java.util.List;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Queen extends Piece{
    private static final byte[] EMPTY_DIRECTION = new byte[0];
    private final byte[][] moveSet = {
            EMPTY_DIRECTION, EMPTY_DIRECTION, EMPTY_DIRECTION, EMPTY_DIRECTION,
            EMPTY_DIRECTION, EMPTY_DIRECTION, EMPTY_DIRECTION, EMPTY_DIRECTION
    };

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
    byte[][] getMoveSet() {
        return moveSet;
    }

    @Override
    public byte[][] getMoves(int pos) {
        List<byte[]> directions = new ArrayList<>();

        int dLeft = distanceLeftB(pos);
        int dTop = distanceTopB(pos);
        int dRight = distanceRightB(pos);
        int dBott = distanceBottomB(pos);

        if (dLeft > 0) {
            byte[] left = new byte[dLeft];
            for (int i = 1; i <= dLeft; i++) {
                left[i - 1] = (byte) (pos + i * LEFT.offset);
            }
            directions.add(left);
        }

        if (dRight > 0) {
            byte[] right = new byte[dRight];
            for (int i = 1; i <= dRight; i++) {
                right[i - 1] = (byte) (pos + i * RIGHT.offset);
            }
            directions.add(right);
        }

        if (dTop > 0) {
            byte[] up = new byte[dTop];
            for (int i = 1; i <= dTop; i++) {
                up[i - 1] = (byte) (pos + i * UP.offset);
            }
            directions.add(up);
        }

        if (dBott > 0) {
            byte[] down = new byte[dBott];
            for (int i = 1; i <= dBott; i++) {
                down[i - 1] = (byte) (pos + i * DOWN.offset);
            }
            directions.add(down);
        }

        int upLeftLen = Math.min(dLeft, dTop);
        if (upLeftLen > 0) {
            byte[] upLeft = new byte[upLeftLen];
            for (int i = 1; i <= upLeftLen; i++) {
                upLeft[i - 1] = (byte) (pos + i * UP_L.offset);
            }
            directions.add(upLeft);
        }

        int upRightLen = Math.min(dRight, dTop);
        if (upRightLen > 0) {
            byte[] upRight = new byte[upRightLen];
            for (int i = 1; i <= upRightLen; i++) {
                upRight[i - 1] = (byte) (pos + i * UP_R.offset);
            }
            directions.add(upRight);
        }

        int downRightLen = Math.min(dBott, dRight);
        if (downRightLen > 0) {
            byte[] downRight = new byte[downRightLen];
            for (int i = 1; i <= downRightLen; i++) {
                downRight[i - 1] = (byte) (pos + i * DOWN_R.offset);
            }
            directions.add(downRight);
        }

        int downLeftLen = Math.min(dBott, dLeft);
        if (downLeftLen > 0) {
            byte[] downLeft = new byte[downLeftLen];
            for (int i = 1; i <= downLeftLen; i++) {
                downLeft[i - 1] = (byte) (pos + i * DOWN_L.offset);
            }
            directions.add(downLeft);
        }

        return directions.toArray(new byte[0][]);
    }
}
