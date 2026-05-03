package org.mxnik.forcechess.ChessLogic.Notation;
import org.mxnik.forcechess.ChessLogic.Pieces.*;
import org.mxnik.forcechess.FenException;

public class FenConversion {

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


    public static char FromPiece(PieceTypes p, boolean color) throws FenException {
        switch (p){
            case KING -> {
                return (char) ('k' - ((color)? 32 : 0));
            }case BISHOP -> {
                return (char) ('b' - ((color)? 32 : 0));
            }case KNIGHT -> {
                return (char) ('n' - ((color)? 32 : 0));
            }case ROOK -> {
                return (char) ('r' - ((color)? 32 : 0));
            }case QUEEN -> {
                return (char) ('q' - ((color)? 32 : 0));
            }case PAWN -> {
                return (char) ('p' - ((color)? 32 : 0));
            }default -> {
                throw new FenException("got Illegal or Empty piece while trying to convert", p.value);
            }
        }
    }
}
