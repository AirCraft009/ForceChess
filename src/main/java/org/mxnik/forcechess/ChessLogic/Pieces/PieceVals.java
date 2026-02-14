package org.mxnik.forcechess.ChessLogic.Pieces;

public enum PieceVals {
    PAWNW(1),
    PAWNB(1),
    KNIGHTW(3),
    KNIGHTB(3),
    BISHOPW(3),
    BISHOPB(3),
    ROOKW(5),
    ROOKB(5),
    QUEENW(9),
    QUEENB(9),
    KINGW(Integer.MAX_VALUE),
    KINGB(Integer.MAX_VALUE);


    public final int value;
    PieceVals(int val){
        this.value = val;
    }

    public boolean isWhite(){
        return this.ordinal() % 2 == 0;
    }
}
