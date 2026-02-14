package org.mxnik.forcechess.ChessLogic.Notation;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;



public final class FenNotation {
    // sollte hidden sein
    // Board API sollte readFen and writeFen exposen

    @Nullable
    @Contract(pure = true)
    public static Piece[] readFen(String fenStr) throws FenException{
        return null;
    }

    @NotNull
    @Contract(pure = true)
    public static String writeFen(Piece[] board){
        return "null";
    }
}
