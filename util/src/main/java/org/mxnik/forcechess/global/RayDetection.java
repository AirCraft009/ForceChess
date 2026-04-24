package org.mxnik.forcechess.global;

public final class RayDetection {

    // Ray directions as {rowDelta, colDelta}.
    // 0-3: straight (rank/file) — rook / queen
    // 4-7: diagonal             — bishop / queen / pawn
    public static final int[] RAY_ROW = { 0,  0,  1, -1,  1,  1, -1, -1 };
    public static final int[] RAY_COL = { 1, -1,  0,  0,  1, -1,  1, -1 };

    // Knight jump deltas
    public static final int[] KNIGHT_ROW = { 2,  2, -2, -2,  1,  1, -1, -1 };
    public static final int[] KNIGHT_COL = { 1, -1,  1, -1,  2, -2,  2, -2 };
}
