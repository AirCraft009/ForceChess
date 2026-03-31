package org.mxnik.forcechess.user.ChessLogic.Moves;

import org.mxnik.forcechess.user.ChessLogic.Board;
import org.mxnik.forcechess.user.ChessLogic.Pieces.*;

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
        DOWN_L.offset = (byte) (DOWN.offset - 1);
        DOWN_R.offset = (byte) (DOWN.offset + 1);
        UP_L.offset = (byte) (UP.offset - 1);
        UP_R.offset = (byte) (UP.offset + 1);

        Piece.refreshMoveSets();
        King.refreshMoveSets();
        Queen.refreshMoveSets();
        Pawn.refreshMoveSets();
        Rook.refreshMoveSets();
        Bishop.refreshMoveSets();
        Knight.refreshMoveSets();
    }
}