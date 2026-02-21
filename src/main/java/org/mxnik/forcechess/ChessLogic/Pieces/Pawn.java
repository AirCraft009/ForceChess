package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.Helper;
import org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

import java.util.Arrays;
import java.util.jar.JarEntry;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.Helper.getCol;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Pawn extends Piece{
    private final byte[] moveSet = {
            (byte) (UP.offset * 2),
            UP.offset
    };
    public Pawn(boolean color, boolean hasMoved) {
        super(PieceTypes.PAWN, color, hasMoved);
    }

    @Override
    public boolean isValidMove(int from, int to){
        if(!isInside(to)) return false;

        int dir = color ? 1 : -1;

        int rowFrom = getRow(from);
        int rowTo = getRow(to);

        int colFrom = getCol(from);
        int colTo = getCol(to);

        int rowDiff = rowTo - rowFrom;
        int colDiff = Math.abs(colTo - colFrom);

        // forward move
        if(colDiff == 0 && rowDiff == dir)
            return true;

        // capture move
        if(colDiff == 1 && rowDiff == dir)
            return true;

        return !hasMoved && colDiff == 0 && rowDiff == dir * 2;
    }

    @Override
    byte[] getMoveSet() {
        return moveSet;
    }

    public static void main(String[] args) {
        Pawn p = new Pawn(true, false);
        Knight k = new Knight(true, false);
        Rook r = new Rook(true, false);
        Piece p1 = p;
        Piece p2 = k;
        Piece p3 = r;
        System.out.println(Arrays.toString(p1.getMoves(8)));
        System.out.println(Arrays.toString(p2.getMoves(28)));
        System.out.println(Arrays.toString(p3.getMoves(36)));
    }

}
