package org.mxnik.forcechess.Util.Notation;

import org.mxnik.forcechess.user.ChessLogic.Board;
import org.mxnik.forcechess.user.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.user.ChessLogic.Pieces.PieceTypes;

public class FenWriter {

    public static String WriteFen(Board board){
        StringBuilder fenBuilder = new StringBuilder();
        String sideLen =  Integer.toString(Board.sideLen);
        char turn = (board.getTurn())? 'w' : 'b';
        Piece[] pieceBoard = board.getBoard();
        int skip = 0;
        int ptr = 0;

        rowloop:
        for (int i = Board.sideLen - 1; i > 0 ; i--) {
            for (int j = 0; j < Board.sideLen; j++) {
                ptr = i * Board.sideLen + j;

                if(pieceBoard[ptr].getType() == PieceTypes.EMPTY){
                    while (pieceBoard[ptr].getType() == PieceTypes.EMPTY){
                        skip ++;
                        j ++;
                        ptr ++;
                        if ((ptr) % Board.sideLen == 0){
                            fenBuilder.append(skip);
                            fenBuilder.append('\\');
                            skip = 0;
                            continue rowloop;
                        }
                    }
                    continue;
                }

                Piece piece = pieceBoard[ptr];
                if(skip != 0) {
                    fenBuilder.append(skip);
                    skip = 0;
                }
                char s = FenConversion.FromPiece(piece.getType(), piece.getColor());
                fenBuilder.append(s);

                if ((ptr + 1) % Board.sideLen == 0){
                    fenBuilder.append('\\');
                }

            }
        }

        //remove the last slash
        fenBuilder.deleteCharAt(fenBuilder.length()-1);
        fenBuilder.append(' ');
        fenBuilder.append(turn);
        fenBuilder.append(' ');
        // temporary 0's
        fenBuilder.append("0 0 0");
        fenBuilder.append(' ');
        fenBuilder.append(sideLen);

        return fenBuilder.toString();
    }

    public static void main(String[] args) {
        Board b = new Board();
        System.out.println(b.toStringBoard());
        System.out.println(WriteFen(b));
    }
}
