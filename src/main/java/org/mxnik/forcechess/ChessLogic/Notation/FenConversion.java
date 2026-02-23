package org.mxnik.forcechess.ChessLogic.Notation;

import org.mxnik.forcechess.ChessLogic.Pieces.*;

import static org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes.*;

class FenConversion {

    public static Piece FromFen(char c){
        return switch (c) {
            case 'p' -> new Pawn(false, false);
            case 'b' -> new Bishop(false, false);
            case 'n' -> new Knight(false, false);
            case 'r' -> new Rook(false, false);
            case 'q' -> new Queen(false, false);
            case 'k' -> new King(false, false);
            case 'P' -> new Pawn(true, false);
            case 'B' -> new Bishop(true, false);
            case 'N' -> new Knight(true, false);
            case 'R' -> new Rook(true, false);
            case 'Q' -> new Queen(true, false);
            case 'K' -> new King(true, false);
            default -> IllegalPiece.ILLEGAL_PIECE;
        };
    }
}
