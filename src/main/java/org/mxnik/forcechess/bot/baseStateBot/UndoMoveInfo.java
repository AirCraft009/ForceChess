package org.mxnik.forcechess.bot.baseStateBot;


// Info to unmake a move packed into an int
// bits 0  - 19 : Move (see bit desc. in Move class)
// bits 20 - 23 : TakenPiece (Type and Color of the taken piece -> Empty Piece if none were taken
public final class UndoMoveInfo {

    public static int of(int move, int takenPiece){
        return move | takenPiece << 20;
    }

    public static int move(int info){
        return info & MOVE_MASK;
    }

    public static int takenPiece(int info){
        return (info >> TAKEN_PIECE_SHIFT) & TAKEN_PIECE_MASK;
    }

    private final static int MOVE_MASK = 0x7FFFF;
    private final static int TAKEN_PIECE_MASK = 0xF;

    private final static int TAKEN_PIECE_SHIFT = 20;

}
