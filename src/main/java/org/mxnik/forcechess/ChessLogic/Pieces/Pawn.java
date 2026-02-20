package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;
import java.util.jar.JarEntry;

import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Pawn extends Piece{

    public Pawn(byte color, boolean hasMoved) {
        super(PieceTypes.PAWN, color, hasMoved);
    }

    @Override
    public byte[] getMoves(int pos) {
        if (!hasMoved){
            return new byte[]
                    {
                            UP.offset,
                            (byte) (UP.offset * 2),
                    };
        }

        return new byte[]{
                UP.offset
        };
    }
}
