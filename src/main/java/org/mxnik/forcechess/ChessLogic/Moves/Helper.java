package org.mxnik.forcechess.ChessLogic.Moves;

import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

public class Helper {

    /**
     * get the row of a position
     */
    public static int getRow(int pos){
        return pos / Board.sideLen;
    }

    /***
     * get the column of a position
     */
    public static int getCol(int pos){
        return pos % Board.sideLen;
    }

    /**
     * get the distance to the left border;
     */
    public static int distanceLeftB(int pos){
        return ((distanceRightB(pos) - Board.sideLen) + 1) * -1;
    }

    /**
     * get the distance to the right border;
     */
    public static int distanceRightB(int pos){
        return Board.sideLen - (getCol(pos) + 1);
    }

    /**
     * get the distance to the top of the board;
     */
    public static int distanceTopB(int pos){
        return (Board.sideLen - getRow(pos)) * Board.sideLen;
    }


    /**
     * get the distance to the bottom of the board;
     */
    public static int distanceBottomB(int pos){
        return getRow(pos) * Board.sideLen;
    }

    public static boolean isInside(int pos){
        return pos >= 0 && pos < Board.sideLen * Board.sideLen;
    }

    public static int rowDiff(int from, int to){
        return Math.abs(getRow(from) - getRow(to));
    }

    public static int colDiff(int from, int to){
        return Math.abs(getCol(from) - getCol(to));
    }

    public static boolean isValidKnightMove(int from, int to){
        if(!isInside(to)) return false;

        int r = rowDiff(from, to);
        int c = colDiff(from, to);

        return (r == 2 && c == 1) || (r == 1 && c == 2);
    }

    public static boolean isValidKingMove(int from, int to){
        if(!isInside(to)) return false;

        int r = rowDiff(from, to);
        int c = colDiff(from, to);

        return r <= 1 && c <= 1 && (r + c) != 0;
    }

    public static boolean isValidRookMove(int from, int to){
        if(!isInside(to)) return false;

        return getRow(from) == getRow(to)
                || getCol(from) == getCol(to);
    }

    public static boolean isValidBishopMove(int from, int to){
        if(!isInside(to)) return false;

        return rowDiff(from, to) == colDiff(from, to);
    }

    public static boolean isValidQueenMove(int from, int to){
        if(!isInside(to)) return false;

        int r = rowDiff(from, to);
        int c = colDiff(from, to);

        return r == c || r == 0 || c == 0;
    }

    public static boolean isValidPawnMove(int from, int to, boolean white){
        if(!isInside(to)) return false;

        int dir = white ? -1 : 1;

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

        return false;
    }
}
