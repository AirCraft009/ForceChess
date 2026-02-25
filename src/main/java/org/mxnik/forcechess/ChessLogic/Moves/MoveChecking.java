package org.mxnik.forcechess.ChessLogic.Moves;

import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.Util.FastBitmap;

public class MoveChecking {

    public static MoveTypes CheckMove(Board board, int from, int to){
        return MoveTypes.GoodMove;
    }

    public static byte[] extractPossibleMoves(FastBitmap board, byte[] pieceMovePositions){
        return null;
    }
}