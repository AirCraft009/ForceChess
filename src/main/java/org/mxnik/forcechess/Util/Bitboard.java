package org.mxnik.forcechess.Util;

import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

import java.util.function.IntConsumer;

import static org.mxnik.forcechess.ChessLogic.Notation.FenConversion.FromPiece;

/**
 * Bitboard with helper functions
 * public long (64 spaces) for efficiency
 *
 *
 */
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

    /**
     * Iterate over all set squares — core of move generation
     * @param fn generation function
     */
    public void forEach(IntConsumer fn) {
        long b = board;
        while (b != 0L) {
            fn.accept(Long.numberOfTrailingZeros(b));
            b &= b - 1;
        }
    }

    public static String  visualiseBitboard(Bitboard board){
        StringBuilder sb = new StringBuilder();
        String horizontalLine = "+" + ("---+").repeat(8) + "\n";

        for (int rank = 8 - 1; rank >= 0; rank--) {
            sb.append(horizontalLine);
            sb.append("|");
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                boolean exist = board.get(index);

                char symbol = ' ';
                if(exist){
                    symbol = 'x';
                }
                sb.append(" ").append(symbol).append(" |");
            }
            sb.append(" ").append(rank + 1).append("\n");
        }

        sb.append(horizontalLine);
        sb.append(" ");
        for (int file = 0; file < 8; file++) {
            sb.append(" ").append((char) ('a' + file)).append("  ");
        }
        sb.append("\n");

        return sb.toString();
    }
}
