package org.mxnik.forcechess.ChessLogic.Moves;

public final class MoveList {

    private final int[] moves;

    private final int[] directionOffsets;
    private final int[] directionLengths;

    private final int[] pieceDirOffsets;
    private final int[] pieceDirCounts;

    private int moveCount = 0;
    private int directionCount = 0;
    private int pieceCount = 0;

    public MoveList(int maxPieces, int maxDirections, int maxMoves) {

        moves = new int[maxMoves];

        directionOffsets = new int[maxDirections];
        directionLengths = new int[maxDirections];

        pieceDirOffsets = new int[maxPieces];
        pieceDirCounts = new int[maxPieces];
    }

    // Start a new piece
    public int startPiece() {
        int p = pieceCount;
        pieceDirOffsets[p] = directionCount;
        pieceDirCounts[p] = 0;
        pieceCount ++;
        return p;
    }

    // Start a new direction for current piece
    public int startDirection() {
        int d = directionCount;
        directionOffsets[d] = moveCount;
        directionLengths[d] = 0;
        pieceDirCounts[pieceCount - 1]++;
        directionCount++;
        return d;
    }

    // Add move to current direction
    public void addMove(int square) {
        moves[moveCount] = square;
        moveCount++;
        directionLengths[directionCount - 1]++;
    }

    public void addMoves(int ... squares) {
        for (int square: squares) {
            moves[moveCount] = square;
            moveCount++;
            directionLengths[directionCount - 1]++;
        }
    }

    // Accessors

    public int getMoveCount(){
        return moveCount;
    }

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

    public int[] getMovesArray() {
        return moves;
    }

    public void clear() {
        moveCount = 0;
        directionCount = 0;
        pieceCount = 0;
    }
}