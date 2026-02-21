package org.mxnik.forcechess.ChessLogic.Moves;
import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;

public class temp {




    public static boolean isValidBishopMove(int from, int to){
        if(!isInside(to)) return false;

        return rowDiff(from, to) == colDiff(from, to);
    }

}
