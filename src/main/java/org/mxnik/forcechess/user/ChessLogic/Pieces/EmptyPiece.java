package org.mxnik.forcechess.user.ChessLogic.Pieces;

public final class EmptyPiece extends Piece{
    public static final EmptyPiece EMPTY_PIECE= new EmptyPiece();

    public EmptyPiece() {
        super(PieceTypes.EMPTY, true);
    }

    @Override
    boolean isValidMove(int from, int to) {
        return false;
    }

    @Override
    byte[] getMoveSet() {
        return new byte[0];
    }

    @Override
    public int getMaxDir() {
        return 0;
    }
}

