package org.mxnik.forcechess.ChessLogic.Notation;

import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import static org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes.*;

class FenConversion {

    public static Piece FromFen(char c){
        switch (c){
            case 'p':
                return new Piece(PAWN, (short) 1);
            case 'b':
                return new Piece(BISHOP, (short) 1);
            case 'n':
                return new Piece(KNIGHT, (short) 1);
            case 'r':
                return new Piece(ROOK, (short) 1);
            case 'q':
                return new Piece(QUEEN, (short) 1);
            case 'k':
                return new Piece(KING, (short) 1);
            case 'P':
                return new Piece(PAWN, (short) 0);
            case 'B':
                return new Piece(BISHOP, (short) 0);
            case 'N':
                return new Piece(KNIGHT, (short) 0);
            case 'R':
                return new Piece(ROOK, (short) 0);
            case 'Q':
                return new Piece(QUEEN, (short) 0);
            case 'K':
                return new Piece(KING, (short) 0);
            default:
                return new Piece(ILLEGAL, (short) -1);
        }
    }
}
