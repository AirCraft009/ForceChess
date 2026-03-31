package org.mxnik.forcechess.Util;

import org.mxnik.forcechess.ChessLogic.Board;

import static java.lang.Math.abs;

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
        return (getCol(pos));
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
        return abs(getRow(from) - getRow(to));
    }

    public static int colDiff(int from, int to){
        return abs(getCol(from) - getCol(to));
    }


    public static boolean isDiagonalMove(int from, int to){
        int moveOffset = abs(from - to);

        return (moveOffset == Board.sideLen - 1) || (moveOffset == Board.sideLen + 1);
    }

    public static boolean contains(byte[] arr, int moveField){
        for (byte val : arr){
            if(moveField == val){
                return true;
            }
        }

        return false;
    }
}
