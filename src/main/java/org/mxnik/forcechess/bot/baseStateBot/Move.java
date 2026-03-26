package org.mxnik.forcechess.bot.baseStateBot;

// A move packed into one int:
// bits 0-5:  from square
// bits 6-11: to square
// bits 12-15: flags (capture, castle, en passant, promotion piece)
public final class Move {
    public static int of(int from, int to, int flags) {
        return from | to << TO_MOVE_SHIFT | flags << FLAG_SHIFT;
    }
    public static int from(int move)  { return move & MOVE_MASK; }
    public static int to(int move)    { return (move >>> TO_MOVE_SHIFT) & MOVE_MASK; }
    public static int flags(int move) { return (move >>> FLAG_SHIFT) & 0xF; }

    public static final int FLAG_GENERIC = 0;
    public static final int FLAG_CAPTURE   = 1;
    public static final int FLAG_CASTLE    = 2;
    public static final int FLAG_EN_PASSANT = 3;
    public static final int FLAG_PROMOTE_Q = 4;

    public static final int MOVE_MASK = 0x3F;
    public static final int TO_MOVE_SHIFT = 6;
    public static final int FLAG_SHIFT = 12;

    // out of bounds checkers.

    // R-Border
    public static final long FILE_A = 0x0101010101010101L;
    // L-Border
    public static final long FILE_H = 0x0808080808080808L;
    // FILE_A and B
    static final long FILE_AB = FILE_A | (FILE_A << 1);
    //FILE_A and H
    static final long FILE_GH = FILE_H | (FILE_H >> 1);

    // where to jump to on every square
    public static final long[] KNIGHT_LOOKUP = new long[64];

    static {
        for (int sq = 0; sq < 64; sq++) {
            long b = 1L << sq;
            KNIGHT_LOOKUP[sq] =
                    ((b << 17) & ~FILE_A)                   // up 2, right 1
                            | ((b << 15) & ~FILE_H)         // up 2, left 1
                            | ((b << 10) & ~FILE_AB)        // up 1, right 2
                            | ((b <<  6) & ~FILE_GH)        // up 1, left 2
                            | ((b >>  6) & ~FILE_AB)        // down 1, right 2
                            | ((b >> 10) & ~FILE_GH)        // down 1, left 2
                            | ((b >> 15) & ~FILE_A)         // down 2, right 1
                            | ((b >> 17) & ~FILE_H);        // down 2, left 1
        }
    }


}