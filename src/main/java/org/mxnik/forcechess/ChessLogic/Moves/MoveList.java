package org.mxnik.forcechess.ChessLogic.Moves;

public final class MoveList {

    private final byte[] moves;

    private final int[] directionOffsets;
    private final int[] directionLengths;

    private final int[] pieceDirOffsets;
    private final int[] pieceDirCounts;

    private int moveCount = 0;
    private int directionCount = 0;
    private int pieceCount = 0;

    public MoveList(int maxPieces, int maxDirections, int maxMoves) {

        moves = new byte[maxMoves];

        directionOffsets = new int[maxDirections];
        directionLengths = new int[maxDirections];

        pieceDirOffsets = new int[maxPieces];
        pieceDirCounts = new int[maxPieces];
    }

    // Start a new piece
    public int startPiece() {
        int p = pieceCount;
        pieceCount++;
        pieceDirOffsets[p] = directionCount;
        pieceDirCounts[p] = 0;
        return p;
    }

    // Start a new direction for current piece
    public int startDirection() {
        int d = directionCount;
        directionCount++;
        directionOffsets[d] = moveCount;
        directionLengths[d] = 0;
        pieceDirCounts[pieceCount - 1]++;
        return d;
    }

    // Add move to current direction
    public void addMove(byte square) {
        moves[moveCount++] = square;
        directionLengths[directionCount - 1]++;
    }

    public void addMoves(byte ... squares) {
        for (byte square: squares) {
            moves[moveCount++] = square;
            directionLengths[directionCount - 1]++;
        }
    }

    // Accessors

    public int getPieceCount() {
        return pieceCount;
    }

    public int getDirectionOffset(int piece) {
        return pieceDirOffsets[piece];
    }

    public int getDirectionCount(int piece) {
        return pieceDirCounts[piece];
    }

    public int getDirectionMovesOffset(int direction) {
        return directionOffsets[direction];
    }

    public int getDirectionMovesLength(int direction) {
        return directionLengths[direction];
    }

    public byte[] getMovesArray() {
        return moves;
    }

    public void clear() {
        moveCount = 0;
        directionCount = 0;
        pieceCount = 0;
    }
}