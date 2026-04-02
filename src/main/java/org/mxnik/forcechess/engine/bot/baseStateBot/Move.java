package org.mxnik.forcechess.engine.bot.baseStateBot;

// A move packed into one int:
// bits 0  - 5 :  from square
// bits 6  - 11: to square
// bits 12 - 15: flags (capture, castle, en passant, promotion piece)
// └─> bits(12-14) Type, bit 15 (is a capture)

public final class Move {
    /**
     * encodes a move into a single integer
     * @param from start square
     * @param to end square
     * @param flags Move flags (capture, castle etc..)
     * @return encoded move
     */
    public static int of(int from, int to, int flags) {
        return from | to << TO_MOVE_SHIFT | flags << FLAG_SHIFT;
    }
    // getter methods
    public static int from(int move)  { return move & MOVE_MASK; }
    public static int to(int move)    { return (move >>> TO_MOVE_SHIFT) & MOVE_MASK; }
    public static int flags(int move) { return (move >>> FLAG_SHIFT) & FLAG_MASK; }

    public static boolean attackFromFlag(int flag){ return ((flag >>> 3) & 0x1) == 1L;}
    public static int baseFlag(int flag){ return (flag & 0x7);}


    // Shifts and masks
    private static final int TO_MOVE_SHIFT  = 6;
    private static final int MOVE_MASK      = 0x3F;

    private static final int FLAG_SHIFT     = 12;
    private static final int FLAG_MASK      = 0xF;



    // Flags (bit 3 -> attack bit); (bits 0-2 ->
    public static final int CAPTURE_BIT     = 8;

    public static final int FLAG_GENERIC    = 0;
    public static final int FLAG_CASTLE_K   = 1;// no legal capture variant
    public static final int FLAG_CASTLE_Q   = 2;// no legal capture variant
    public static final int FLAG_EN_PASSANT = 3;// illegal state only for checks
    public static final int FLAG_PROMOTE_Q  = 4;
    public static final int FLAG_PROMOTE_R  = 5;
    public static final int FLAG_PROMOTE_B  = 6;
    public static final int FLAG_PROMOTE_N  = 7;

    // capture-moves
    public static final int FLAG_GENERIC_CAPTURE    = 8;
    public static final int FLAG_CASTLE_K_CAPTURE   = 9;   // illegal state only for checks
    public static final int FLAG_CASTLE_Q_CAPTURE   = 0xA; // illegal state only for checks
    public static final int FLAG_EN_PASSANT_CAPTURE = 0xB; // no legal non-capture variant
    public static final int FLAG_PROMOTE_Q_CAPTURE  = 0xC;
    public static final int FLAG_PROMOTE_R_CAPTURE  = 0xD;
    public static final int FLAG_PROMOTE_B_CAPTURE  = 0xE;
    public static final int FLAG_PROMOTE_N_CAPTURE  = 0xF;



    // out of bounds checkers.
    // R-Border
    public static final long FILE_A = 0x0101010101010101L;
    // L-Border
    public static final long FILE_H = 0x8080808080808080L;
    // FILE_A and B
    static final long FILE_AB = FILE_A | (FILE_A << 1);
    //FILE_A and H
    static final long FILE_GH = FILE_H | (FILE_H >>> 1);

    // Top-Border
    public static final long ROW_8 = 0xFF00000000000000L;
    // Bottom-Border
    public static final long ROW_1 = 0x00000000000000FFL;

    // where to jump to on every square
    public static final long[] KNIGHT_LOOKUP = new long[64];
    public static final long[] KING_LOOKUP = new long[64];

    static {
        for (int sq = 0; sq < 64; sq++) {
            long b = 1L << sq;
            KNIGHT_LOOKUP[sq] =
                    ((b << 17) & ~FILE_A)                   // up 2, right 1
                            | ((b << 15) & ~FILE_H)         // up 2, left 1
                            | ((b << 10) & ~FILE_AB)        // up 1, right 2
                            | ((b <<  6) & ~FILE_GH)        // up 1, left 2
                            | ((b >>>  6) & ~FILE_AB)        // down 1, right 2
                            | ((b >>> 10) & ~FILE_GH)        // down 1, left 2
                            | ((b >>> 15) & ~FILE_A)         // down 2, right 1
                            | ((b >>> 17) & ~FILE_H);        // down 2, left 1
        }

        for (int sq = 0; sq < 64; sq++) {
            long b = 1L << sq;
            KING_LOOKUP[sq] =
                    ((b << 8) & ~ROW_1)                             // up
                            | ((b >>> 8) & ~ROW_8)                  // down
                            | ((b << 1) & ~FILE_A)                  // right
                            | ((b >>> 1) & ~FILE_H)                 // left
                            | ((b << 9) & ~(ROW_1 | FILE_A))        // up right
                            | ((b << 7) & ~(ROW_1 | FILE_H))        // up left
                            | ((b >>> 7) & ~(ROW_8 | FILE_A))       // down right
                            | ((b >>> 9) & ~(ROW_8 | FILE_H));      // down left
        }
    }


}