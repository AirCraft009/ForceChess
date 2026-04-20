package org.mxnik.forcechess.Pos;

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
    public static boolean color(int Piece){ return ((Piece >>> 3) & 0x1) == 0L; }
    public static int colorInt(int Piece){ return ((Piece >>> 3) & 0x1);}
    public static int pieceT(int Piece){ return (Piece) & 0x7;}
    public static int of(boolean color, int PieceT){return (!color)? PieceT | 1 << 3 : PieceT;}
    public static int of(int color, int PieceT){return PieceT | color << 3;}
}
