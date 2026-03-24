package org.mxnik.forcechess.Util;

import java.util.function.IntConsumer;

public final class Bitboard {

    public long board; // just a long, public for performance

    public Bitboard(long l) {
        board = l;
    }

    public Bitboard(){}

    // Move a piece
    public void set(int square)   { board |=  (1L << square); }
    public void clear(int square) { board &= ~(1L << square); }
    public boolean get(int square){ return (board >>> square & 1L) == 1L; }

    // Chess-specific ops — these are where the value is
    public boolean isEmpty()          { return board == 0L; }
    public int     popCount()         { return Long.bitCount(board); }
    public int     lsb()              { return Long.numberOfTrailingZeros(board); }
    public int     popLsb()           {
        int sq = lsb();
        board &= board - 1;  // clears lowest set bit
        return sq;
    }

    // Iterate over all set squares — core of move generation
    public void forEach(IntConsumer fn) {
        long b = board;
        while (b != 0L) {
            fn.accept(Long.numberOfTrailingZeros(b));
            b &= b - 1;
        }
    }
}
