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
        Arrays.fill(board, Piece.emptyPiece);

        int ptr = boardSize;
        int charptr = 0;
        int fieldsinRow = 0;
        int factor = 1;

        for (int i = 0; i < sideLen; i++) {
            ptr -= sideLen;
            for (int j = 0; j < sideLen; j++) {
                char c = chars[charptr];
                charptr ++;
                if (c == '/'){
                    factor = 1;
                    continue;
                }

                if (Character.isDigit(c)){
                    if (factor > 10){
                        throw new FenException("Skip number bigger than 99", charptr);
                    }
                    int skip = (c - '0') * factor;
                    System.out.println(skip);
                    i += skip / sideLen;
                    ptr -= (skip / sideLen) * sideLen - 1;
                    j += skip % sideLen;
                    factor *= 10;
                    continue;
                }

                Piece p =  FenConversion.FromFen(c);
                if (p.getType() == PieceTypes.ILLEGAL){
                    throw new FenException("Illegal char found in fenStr: " + c, charptr);
                }

                board[ptr +  j] = p;
            }
        }

//        if (ptr != -sideLen){
//            throw new FenException("Fen isn't complete: ", ptr);
//        }
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
