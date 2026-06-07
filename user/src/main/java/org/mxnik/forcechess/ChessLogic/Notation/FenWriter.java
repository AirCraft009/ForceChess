package org.mxnik.forcechess.ChessLogic.Notation;

import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

public class FenWriter {

    public static String WriteFen(Board board){
        return WriteFen(board.getBoard(), board.getTurn(), Board.sideLen, board.getEnPassantPos());
    }

    public static String WriteFen(Piece[] pieceBoard, boolean whiteTurn, int sideLen, int enPassant){
        StringBuilder fenBuilder = new StringBuilder();
        String sideLenStr =  Integer.toString(sideLen);
        char turn = whiteTurn? 'w' : 'b';
        int skip = 0;
        int ptr = 0;

        rowloop:
        for (int i = sideLen - 1; i >= 0 ; i--) {
            for (int j = 0; j < sideLen; j++) {
                ptr = i * sideLen + j;

                while (pieceBoard[ptr].getType() == PieceTypes.EMPTY){
                    skip ++;
                    j ++;
                    ptr ++;
                    if ((ptr) % sideLen == 0){
                        fenBuilder.append(skip);
                        fenBuilder.append('/');
                        skip = 0;
                        continue rowloop;
                    }
                }

                Piece piece = pieceBoard[ptr];
                System.out.println(piece);
                if(skip != 0) {
                    fenBuilder.append(skip);
                    skip = 0;
                }
                char s = FenConversion.FromPiece(piece.getType(), piece.getColor());
                fenBuilder.append(s);

                if ((ptr + 1) % sideLen == 0){
                    fenBuilder.append('/');
                }

            }
        }

        //remove the last slash
        fenBuilder.deleteCharAt(fenBuilder.length()-1);
        fenBuilder.append(' ');
        fenBuilder.append(turn);
        fenBuilder.append(' ');
        // temporary 0's
        fenBuilder.append("KQkq ").append(enPassant).append(" 0");//TODO swap out 0's
        fenBuilder.append(' ');
        fenBuilder.append(sideLenStr);

        return fenBuilder.toString();
    }

    public static void main(String[] args) {
        Board b = new Board();
        System.out.println(b.toStringBoard());
        System.out.println(WriteFen(b));
    }
}
