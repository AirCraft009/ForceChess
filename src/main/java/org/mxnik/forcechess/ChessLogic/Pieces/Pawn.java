package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.MoveList;
import org.mxnik.forcechess.Util.Helper;
import org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

import java.util.Arrays;
import java.util.jar.JarEntry;

import static org.mxnik.forcechess.Util.Helper.*;
import static org.mxnik.forcechess.Util.Helper.getCol;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Pawn extends Piece{
    public final static int dirCount = 1;
    private final byte[] moveSet = {
            UP.offset,
            (byte) (UP.offset * 2),
    };
    public Pawn(boolean color, boolean hasMoved) {
        super(PieceTypes.PAWN, color, hasMoved);
    }

    @Override
    public int getMaxDir() {
        return dirCount;
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
        Bishop b = new Bishop(true, false);
        Piece[] pieces = new Piece[] {p, k, r, b};
        int[] positions = new int[] {8, 28, 36, 36};

        MoveList moveList = new MoveList(pieces.length, 24, 64);

        for (int i = 0; i < pieces.length; i++) {
            pieces[i].getMoves(positions[i], moveList);
        }

        System.out.println(Arrays.toString(moveList.getMovesArray()));
    }

}
