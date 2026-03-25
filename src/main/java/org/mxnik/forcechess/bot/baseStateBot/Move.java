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
    public static int flags(int move) { return (move >>> 12) & 0xF; }

    public static final int FLAG_NONE      = 0;
    public static final int FLAG_CAPTURE   = 1;
    public static final int FLAG_CASTLE    = 2;
    public static final int FLAG_EN_PASSANT = 3;
    public static final int FLAG_PROMOTE_Q = 4;

    public static final int MOVE_MASK = 0x3F;
    public static final int TO_MOVE_SHIFT = 6;
    public static final int FLAG_SHIFT = 12;
}