package org.mxnik.forcechess.ChessLogic.Pieces;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Knight extends Piece{
    private static final byte[] moveSet = {
            (byte) (RIGHT.offset + UP.offset * 2),
            (byte) (LEFT.offset + UP.offset * 2),
            (byte) (RIGHT.offset + DOWN.offset * 2),
            (byte) (LEFT.offset + DOWN.offset * 2),

            (byte) (RIGHT.offset * 2 + UP.offset),
            (byte) (LEFT.offset * 2 + UP.offset),
            (byte) (RIGHT.offset * 2 + DOWN.offset),
            (byte) (LEFT.offset * 2+ DOWN.offset),
    };

    public Knight(byte color, boolean hasMoved) {
        super(PieceTypes.KNIGHT, color, hasMoved);
    }


}
