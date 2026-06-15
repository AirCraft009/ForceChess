package org.mxnik.forcechess.ChessLogic.Pieces;

public final class IllegalPiece extends Piece {
    public static final IllegalPiece ILLEGAL_PIECE = new IllegalPiece();

    public IllegalPiece() {
        super(PieceTypes.ILLEGAL, true);
    }

    @Override
    boolean isValidMove(int from, int to) {
        return false;
    }

    @Override
    int[] getMoveSet() {
        return new int[0];
    }

    @Override
    public int getMaxDir() {
        return 0;
    }
}
