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
        return (Board.sideLen - getRow(pos)) - 1;
    }


    /**
     * get the distance to the bottom of the board;
     */
    public static int distanceBottomB(int pos){
        return getRow(pos);
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

}
