package org.mxnik.forcechess.ChessLogic.Notation;

import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.CastleRights;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

public class FenWriter {

    public static String WriteFen(Board board){
        return WriteFen(board.getBoard(), board.getTurn(), board.getFiftyMove(), Board.sideLen, board.getEnPassantPos(), board.getCastleRights());
    }
    //TODO: write extra attributes correctly
    public static String WriteFen(Piece[] pieceBoard, boolean whiteTurn, int fiftyMove, int sideLen, int enPassant, CastleRights rights){
        StringBuilder fenBuilder = new StringBuilder();
        String sideLenStr =  Integer.toString(sideLen);
        char turn = whiteTurn? 'w' : 'b';
        int skip = 0;
        int ptr;

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
        fenBuilder.deleteCharAt(fenBuilder.length()-1).append(' ')
        .append(turn).append(' ');

        if(!rights.allNegative()) {
            fenBuilder
                    .append((!rights.WK_Castle ? "" : "K"))
                    .append((!rights.WQ_Castle ? "" : "Q"))
                    .append((!rights.BK_Castle ? "" : "k"))
                    .append((!rights.BQ_Castle ? "" : "q"));
        }
        else {
            fenBuilder.append("-");
        }

        fenBuilder.append(' ');
        fenBuilder.append(FenReader.toFieldName(enPassant)).append(' ')
        .append(fiftyMove).append(' ')
        .append(sideLenStr);

        return fenBuilder.toString();
    }

    public static void main(String[] args) {
        Board b = new Board();
        System.out.println(b.toStringBoard());
        System.out.println(WriteFen(b));
    }
}
