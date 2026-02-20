package org.mxnik.forcechess.ChessLogic.Moves;

import org.mxnik.forcechess.ChessLogic.Board;

public enum MoveOffsets {
    UP(Board.sideLen),
    DOWN(-Board.sideLen),
    LEFT(-1),
    RIGHT(1),
    UP_R(Board.sideLen+1),
    UP_L(Board.sideLen-1),
    DOWN_R(-Board.sideLen+1),
    DOWN_L(-Board.sideLen-1);

    public byte offset;
    MoveOffsets(int offset) {
        this.offset = (byte) offset;
    }

    public static void calculateOffset(int  sidelength) {
        UP.offset = (byte) sidelength;
        DOWN.offset = (byte) -sidelength;
    }
}