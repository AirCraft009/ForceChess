package org.mxnik.forcechess.network_bot_interactions.Pos;

import org.mxnik.forcechess.global.Bitboard;

import java.util.Arrays;

public class PositionUtils {

    /** Shallow clone used in legal-move filter tests. */
    public static PositionEncoder.Position deepCopy(PositionEncoder.Position src) {
        var d = new PositionEncoder.Position();
        d.WPawns = src.WPawns;     d.WKnights = src.WKnights;
        d.WBishops = src.WBishops; d.WRooks   = src.WRooks;
        d.WQueens  = src.WQueens;  d.WKing    = src.WKing;
        d.BPawns   = src.BPawns;   d.BKnights = src.BKnights;
        d.BBishops = src.BBishops; d.BRooks   = src.BRooks;
        d.BQueens  = src.BQueens;  d.BKing    = src.BKing;
        d.Occupied = src.Occupied; d.WPieces  = src.WPieces;
        d.BPieces  = src.BPieces;
        d.WQueenCastle = src.WQueenCastle; d.WKingCastle = src.WKingCastle;
        d.BQueenCastle = src.BQueenCastle; d.BKingCastle = src.BKingCastle;
        d.enPassantSquare  = src.enPassantSquare;
        d.whiteToMove      = src.whiteToMove;
        d.fiftyMoveCounter = src.fiftyMoveCounter;
        d.pieceMap = Arrays.copyOf(src.pieceMap, 64);
        return d;
    }

    /**
     * Place a piece and keep pieceMap + bitboards + aggregate boards in sync.
     *   color : true = white, false = black
     *   type  : Piece.PAWN / KNIGHT / BISHOP / ROOK / QUEEN / KING
     */
    public static void place(PositionEncoder.Position pos, boolean color, int type, int sq) {
        // Piece.of(color, type) must produce the byte stored in pieceMap.
        int piece = Piece.of(color, type);
        pos.pieceMap[sq] = (byte) piece;

        long bit = Bitboard.set(0L, sq);
        if (color) {
            switch (type) {
                case Piece.PAWN   -> pos.WPawns   |= bit;
                case Piece.KNIGHT -> pos.WKnights |= bit;
                case Piece.BISHOP -> pos.WBishops |= bit;
                case Piece.ROOK   -> pos.WRooks   |= bit;
                case Piece.QUEEN  -> pos.WQueens  |= bit;
                case Piece.KING   -> pos.WKing    |= bit;
            }
            pos.WPieces |= bit;
        } else {
            switch (type) {
                case Piece.PAWN   -> pos.BPawns   |= bit;
                case Piece.KNIGHT -> pos.BKnights |= bit;
                case Piece.BISHOP -> pos.BBishops |= bit;
                case Piece.ROOK   -> pos.BRooks   |= bit;
                case Piece.QUEEN  -> pos.BQueens  |= bit;
                case Piece.KING   -> pos.BKing    |= bit;
            }
            pos.BPieces |= bit;
        }
        pos.Occupied |= bit;
    }
}
