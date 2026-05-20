package org.mxnik.forcechess.ChessLogic.Moves;


import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.Moves.MovePacket;

public record UndoMovePacket(MovePacket packet, Piece takenP, int fiftyMoveCounter) {
}
