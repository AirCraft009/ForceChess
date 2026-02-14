package org.mxnik.forcechess.ChessLogic.Notation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

import java.util.Arrays;


public final class FenNotation {
    // sollte hidden sein
    // Board API sollte readFen and writeFen exposen


    /**
     * Reads a fenstr and turns it into a board
     * This isn't a normal Fen string information is in the specification markdown
     * @param fenStr - MxNik FenStr
     * @param sideLen - maximally 99x99 field
     * @return a board of pieces
     * @throws FenException exception with the position
     */
    @Nullable
    @Contract(pure = true)
    public static Piece[] readFen(String fenStr, int sideLen) throws FenException{
        int boardSize = sideLen * sideLen;
        char[] chars = fenStr.toCharArray();
        Piece[] board = new Piece[boardSize];
        Arrays.fill(board, new Piece(PieceTypes.EMPTY,  (short) -1, false));

        int ptr = boardSize - 1;
        int fieldsinRow = 0;
        int factor = 1;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '/'){
                if ((fieldsinRow)%sideLen != 0){
                    throw new FenException("Number of fields in line: " + (boardSize - ptr) +  "don't match the sidelen: " + sideLen, i);
                }
                factor = 1;
                fieldsinRow = 0;
                continue;
            }

            // skip die nächsten felder
            if (Character.isDigit(c)){
                if (factor > 10) {
                    throw new FenException("Illegal number of skips, MAX=99", i);
                }
                int off = (c - '0') * factor;
                ptr -= off;
                fieldsinRow += off;
                factor *= 10;
                continue;
            }

            // reset den Faktor
            factor = 1;

            Piece p = FenConversion.FromFen(c);
            if (p.getType() == PieceTypes.ILLEGAL){
                throw new FenException("Illegal Char: " + c + " is not a valid fenStr char", i);
            }

            board[ptr] = p;
            ptr --;
        }

        if (ptr != -1){
            throw new FenException("Fen isn't complete", boardSize);
        }
        return board;
    }

    @NotNull
    @Contract(pure = true)
    public static String writeFen(Piece[] board){
        return "null";
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(readFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", 8)));

    }
}
