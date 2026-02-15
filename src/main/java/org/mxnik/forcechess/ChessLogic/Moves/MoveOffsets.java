package org.mxnik.forcechess.ChessLogic.Moves;

import org.mxnik.forcechess.ChessLogic.Board;

public enum MoveOffsets {
    UP(Board.sideLen), DOWN(-Board.sideLen), LEFT(-1), RIGHT(1);

    int offset;
    MoveOffsets(int offset) {
        this.offset = offset;
    }

    public static void calculateOffset(int sidelength) {
        UP.offset = sidelength;
        DOWN.offset = -sidelength;
    }
}