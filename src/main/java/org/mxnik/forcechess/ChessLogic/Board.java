package org.mxnik.forcechess.ChessLogic;

import org.mxnik.forcechess.ChessLogic.Pieces.Piece;

public class Board {
    Piece[] board;
    short turn;


    Board(int sideLen){
        board = new Piece[sideLen*sideLen];
    }
}
