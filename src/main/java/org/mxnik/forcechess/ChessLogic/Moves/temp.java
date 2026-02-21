package org.mxnik.forcechess.ChessLogic.Moves;
import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;

public class temp {




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
}
