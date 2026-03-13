package org.mxnik.forcechess.ChessLogic.Notation;

import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

public class FenWriter {

    public static String WriteFen(Board board){
        StringBuilder fenBuilder = new StringBuilder();
        String sideLen =  Integer.toString(Board.sideLen);
        String turn = (board.getTurn())? "w" : "b";
        Piece[] pieceBoard = board.getBoard();
        int i = 1;

        for (Piece piece : pieceBoard){
            if(piece.getType() == PieceTypes.EMPTY){
                continue;
            }
            char s = FenConversion.FromPiece(piece.getType(), piece.getColor());
            fenBuilder.append(s);

            if (Board.sideLen % i == 0){

            }
            i++;
        }
        return fenBuilder.toString();
    }
}
