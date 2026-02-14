package org.mxnik.forcechess.ChessLogic.Pieces;

public enum PieceTypes {
    PAWN(1),
    KNIGHT(3),
    BISHOP(3),
    ROOK(5),
    QUEEN(9),
    KING(Integer.MAX_VALUE),
    EMPTY(0),
    ILLEGAL(-1);


    public final int value;
    PieceTypes(int val){
        this.value = val;
    }

}


