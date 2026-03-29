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
    // Move a piece
    public static long set(long board, int square)   { return board |=  (1L << square); }
    public static long clear(long board, int square) { return board &= ~(1L << square); }
    public static boolean get(long board, int square){ return (board >>> square & 1L) == 1L; }

    // Chess-specific ops — these are where the value is
    public static boolean isEmpty(long board)          { return board == 0L; }
    public static int     popCount(long board)         { return Long.bitCount(board); }
    public static int     lsb(long board)              { return Long.numberOfTrailingZeros(board); }
    public static long popLsb(long board)            { return board & (board - 1); } // returns new board, lsb() gets the square

    /**
     * Iterate over all set squares — core of move generation
     * @param fn generation function
     */
    public static void forEach(long board, IntConsumer fn) {
        while (board != 0L) {
            fn.accept(Long.numberOfTrailingZeros(board));
            board &= board - 1;
        }
    }

    public static String  visualiseBitboard(long board){
        StringBuilder sb = new StringBuilder();
        String horizontalLine = "+" + ("---+").repeat(8) + "\n";

        for (int rank = 8 - 1; rank >= 0; rank--) {
            sb.append(horizontalLine);
            sb.append("|");
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                boolean exist = Bitboard.get(board, index);

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
