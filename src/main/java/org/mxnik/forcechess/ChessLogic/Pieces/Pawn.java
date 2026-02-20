package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets;

import java.util.jar.JarEntry;

public class Pawn extends Piece{
    private static final byte[] moveSet = {
            MoveOffsets.UP.offset,
            (byte) (MoveOffsets.UP.offset * 2),
    };
    public Pawn(byte color, boolean hasMoved) {
        super(PieceTypes.PAWN, color, hasMoved);
    }

    @Override
    public byte[] getMoves(){
        return moveSet;
    }
}
