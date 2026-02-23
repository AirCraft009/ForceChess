package org.mxnik.forcechess.ChessLogic.Moves;

import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

import java.util.ArrayList;
import java.util.Arrays;

public class MoveChecking {
    public static MoveTypes CheckMove(Board board, int fromIndex, int toIndex){
        ArrayList<Byte> moves = new ArrayList<>(board.ammountPieces);

        Piece[] boardPieces = board.getBoard();
        for (int i = 0; i < boardPieces.length; i++) {
            Piece p = boardPieces[i];
            if (p.getType() == PieceTypes.EMPTY){
                continue;
            }

            byte[] PieceMoves = p.getMoves(i);
            System.out.println(p+ Arrays.toString(PieceMoves));
        }
        return MoveTypes.GoodMove;
    }
}
