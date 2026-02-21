package org.mxnik.forcechess.ChessLogic.Notation;

import org.mxnik.forcechess.ChessLogic.Pieces.Knight;
import org.mxnik.forcechess.ChessLogic.Pieces.Pawn;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.Rook;

import static org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes.*;

class FenConversion {

    //TODO: update to return Pieces not just piece with type added
    public static Piece FromFen(char c){
        return switch (c) {
            case 'p' -> new Pawn(false, false);
            case 'b' ->
                //TODO: make Bishop class
                    new Pawn(false, false);
            case 'n' -> new Knight(false, false);
            case 'r' -> new Rook(false, false);
            case 'q' ->
                //TODO: make Queen class
                    new Pawn(false, false);
            case 'k' ->
                //TODO: make King class
                    new Pawn(false, false);
            case 'P' -> new Pawn(true, false);
            case 'B' ->
                //TODO: make Bishop class
                    new Pawn(true, false);
            case 'N' -> new Knight(true, false);
            case 'R' -> new Rook(true, false);
            case 'Q' ->
                //TODO: make Queen class
                    new Pawn(true, false);
            case 'K' ->
                //TODO: make King class
                    new Pawn(true, false);
            default -> null;
        };
    }
}
