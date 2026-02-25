package org.mxnik.forcechess.ChessLogic.Pieces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets.*;

public class Pawn extends Piece{
    private final byte[][] moveSet = {
            {UP.offset},
            {(byte) (UP.offset * 2)},
            {UP_L.offset},
            {UP_R.offset}
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

        if(colDiff == 0 && rowDiff == dir) return true;
        if(colDiff == 1 && rowDiff == dir) return true;

        return !hasMoved && colDiff == 0 && rowDiff == dir * 2;
    }

    @Override
    byte[][] getMoveSet() {
        return moveSet;
    }

    @Override
    public byte[][] getMoves(int pos) {
        List<byte[]> directions = new ArrayList<>();

        int dir = color ? 1 : -1;

        int oneForward = pos + dir * UP.offset;
        if (isInside(oneForward)) {
            directions.add(new byte[]{(byte) oneForward});
        }

        if (!hasMoved) {
            int twoForward = pos + dir * UP.offset * 2;
            if (isInside(twoForward)) {
                directions.add(new byte[]{(byte) twoForward});
            }
        }

        int captureLeft = pos + dir * UP_L.offset;
        if (isInside(captureLeft)) {
            directions.add(new byte[]{(byte) captureLeft});
        }

        int captureRight = pos + dir * UP_R.offset;
        if (isInside(captureRight)) {
            directions.add(new byte[]{(byte) captureRight});
        }

        return directions.toArray(new byte[0][]);
    }

    public static void main(String[] args) {
        Pawn p = new Pawn(true, false);
        Knight k = new Knight(true, false);
        Rook r = new Rook(true, false);
        Bishop b = new Bishop(true, false);
        Piece p1 = p;
        Piece p2 = k;
        Piece p3 = r;
        Piece p4 = b;
        System.out.println(Arrays.deepToString(p1.getMoves(8)));
        System.out.println(Arrays.deepToString(p2.getMoves(28)));
        System.out.println(Arrays.deepToString(p3.getMoves(36)));
        System.out.println(Arrays.deepToString(p4.getMoves(36)));
    }

}
