package org.mxnik.forcechess.engine.bot.baseStateBot;

public final class Piece {
    // Color
    public static final int WHITE = 0;
    public static final int BLACK = 1;


    // PieceType

    public static final int EMPTY_PIECE  = 0;
    public static final int ROOK   = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int KING   = 4;
    public static final int QUEEN  = 5;
    public static final int PAWN   = 6;

    //helper
    public static boolean color(int Piece){ return ((Piece) & 0x1) == 1L; }
    public static int pieceT(int Piece){ return (Piece >>> 1) & 0x7;}
    public static int of(boolean color, int PieceT){
        if (color){
            return PieceT;
        }else {
            return PieceT | 1 << 3;
        }
    }
}
