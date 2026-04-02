package engine;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mxnik.forcechess.Util.Bitboard;
import org.mxnik.forcechess.engine.bot.baseStateBot.Move;
import org.mxnik.forcechess.engine.bot.baseStateBot.MoveGen;
import org.mxnik.forcechess.engine.bot.baseStateBot.Piece;
import org.mxnik.forcechess.engine.bot.baseStateBot.PositionEncoder;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// KI-Generiert

/**
 * Unit tests for MoveGen.generateMoves() and MoveGen.generatePseudoMoves().
 *
 * Conventions used here:
 *   - Square indices: sq = row * 8 + col,  row 0 = rank 1 (white back rank),
 *     col 0 = a-file.   a1=0, b1=1, ..., h1=7, a2=8, ..., h8=63.
 *   - Helper sq(col, row) is defined inline below.
 *   - Every test builds its own isolated Position — no shared mutable state.
 *
 * Structure
 * ---------
 *  1.  Utility / setup helpers
 *  2.  White Pawn
 *  3.  Black Pawn
 *  4.  Knight          (wrap-around safety)
 *  5.  Bishop          (diagonals, blocking)
 *  6.  Rook            (straights, blocking)
 *  7.  Queen           (combined)
 *  8.  King            (adjacent squares, wrap-around safety)
 *  9.  Castling
 * 10.  En Passant
 * 11.  Promotion flags
 * 12.  Check / legal-move filter
 * 13.  Full-position integration + perft
 */
@DisplayName("MoveGen")
class MoveGenTest {

    // =========================================================================
    // 1. Utilities
    // =========================================================================

    /** Square index: col in [0,7], row in [0,7]  (row 0 = rank 1). */
    static int sq(int col, int row) { return row * 8 + col; }

    /** Collect destination squares from a move array. */
    static Set<Integer> toSquares(int[] moves) {
        Set<Integer> s = new HashSet<>();
        for (int m : moves) s.add(Move.to(m));
        return s;
    }

    /** True when the array contains at least one move with the given (from, to). */
    static boolean hasMove(int[] moves, int from, int to) {
        for (int m : moves)
            if (Move.from(m) == from && Move.to(m) == to) return true;
        return false;
    }

    /** True when the array contains at least one move with the given (from, to, flags). */
    static boolean hasMove(int[] moves, int from, int to, int flags) {
        for (int m : moves)
            if (Move.from(m) == from && Move.to(m) == to && Move.flags(m) == flags) return true;
        return false;
    }

    /** Count moves whose source square equals `from`. */
    static long countFrom(int[] moves, int from) {
        long c = 0;
        for (int m : moves) if (Move.from(m) == from) c++;
        return c;
    }

    /** Empty position — no pieces, clean state. */
    static PositionEncoder.Position emptyPosition() {
        PositionEncoder.Position pos = new PositionEncoder.Position();
        Arrays.fill(pos.pieceMap, (byte) Piece.EMPTY_PIECE);
        return pos;
    }

    /**
     * Place a piece and keep pieceMap + bitboards + aggregate boards in sync.
     *   color : true = white, false = black
     *   type  : Piece.PAWN / KNIGHT / BISHOP / ROOK / QUEEN / KING
     */
    static void place(PositionEncoder.Position pos, boolean color, int type, int sq) {
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

    /** Run generatePseudoMoves and return only the moves that were added. */
    static int[] genPseudo(PositionEncoder.Position pos, boolean white) {
        int[] buf = new int[256];
        int count = MoveGen.generatePseudoMoves(pos, 0, white, buf);
        return Arrays.copyOf(buf, count);
    }

    /** Run generateMoves (legal) and return only the moves that were added. */
    static int[] genLegal(PositionEncoder.Position pos, boolean white) {
        int[] buf = new int[256];
        int count = MoveGen.generateMoves(pos, 0, white, buf);
        return Arrays.copyOf(buf, count);
    }

    /** Shallow clone used in legal-move filter tests. */
    static PositionEncoder.Position deepCopy(PositionEncoder.Position src) {
        var d = new PositionEncoder.Position();
        d.WPawns = src.WPawns;     d.WKnights = src.WKnights;
        d.WBishops = src.WBishops; d.WRooks   = src.WRooks;
        d.WQueens  = src.WQueens;  d.WKing    = src.WKing;
        d.BPawns   = src.BPawns;   d.BKnights = src.BKnights;
        d.BBishops = src.BBishops; d.BRooks   = src.BRooks;
        d.BQueens  = src.BQueens;  d.BKing    = src.BKing;
        d.Occupied = src.Occupied; d.WPieces  = src.WPieces;
        d.BPieces  = src.BPieces;
        d.WQueenCastlePerm = src.WQueenCastlePerm;
        d.WKingCastlePerm  = src.WKingCastlePerm;
        d.BQueenCastlePerm = src.BQueenCastlePerm;
        d.BKingCastlePerm  = src.BKingCastlePerm;
        d.WQueenCastle = src.WQueenCastle; d.WKingCastle = src.WKingCastle;
        d.BQueenCastle = src.BQueenCastle; d.BKingCastle = src.BKingCastle;
        d.enPassantSquare  = src.enPassantSquare;
        d.whiteToMove      = src.whiteToMove;
        d.fiftyMoveCounter = src.fiftyMoveCounter;
        d.pieceMap = Arrays.copyOf(src.pieceMap, 64);
        return d;
    }

    // =========================================================================
    // 2. White Pawn
    // =========================================================================

    @Nested
    @DisplayName("White Pawn")
    class WhitePawnTests {

        // Helper: minimal position with one white pawn + both kings far away.
        PositionEncoder.Position oneWhitePawn(int col, int row) {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(col, row));
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            pos.WDoublePawnMove = 1L << sq(col, 1);
            return pos;
        }

        @Test
        @DisplayName("Single push from starting rank (row 1)")
        void singlePush() {
            var pos = oneWhitePawn(4, 1); // e2
            assertTrue(hasMove(genPseudo(pos, true), sq(4,1), sq(4,2)), "e2-e3 must exist");
        }

        @Test
        @DisplayName("Double push from starting rank (row 1)")
        void doublePush() {
            var pos = oneWhitePawn(4, 1); // e2
            assertTrue(hasMove(genPseudo(pos, true), sq(4,1), sq(4,3)), "e2-e4 must exist");
        }

        @Test
        @DisplayName("No double push when rank-3 square is occupied")
        void noDoublePushWhenRank3Blocked() {
            var pos = oneWhitePawn(4, 1);
            place(pos, false, Piece.PAWN, sq(4, 2)); // e3 blocker
            int[] m = genPseudo(pos, true);
            assertFalse(hasMove(m, sq(4,1), sq(4,2)), "e2-e3 blocked");
            assertFalse(hasMove(m, sq(4,1), sq(4,3)), "e2-e4 also blocked");
        }

        @Test
        @DisplayName("No double push when rank-4 square is occupied (rank-3 open)")
        void noDoublePushWhenRank4Blocked() {
            var pos = oneWhitePawn(4, 1);
            place(pos, false, Piece.PAWN, sq(4, 3)); // e4 blocker
            int[] m = genPseudo(pos, true);
            assertTrue(hasMove(m, sq(4,1), sq(4,2)), "e2-e3 still open");
            assertFalse(hasMove(m, sq(4,1), sq(4,3)), "e2-e4 blocked");
        }

        @Test
        @DisplayName("No push from non-starting rank generates double-push")
        void noDoublePushFromNonStartingRank() {
            var pos = oneWhitePawn(4, 3); // e5 — not starting rank
            assertFalse(hasMove(genPseudo(pos, true), sq(4,3), sq(4,5)),
                    "Double push only from rank 1");
        }

        @Test
        @DisplayName("Diagonal captures both sides")
        void diagonalCaptures() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(4, 4)); // e5
            place(pos, false, Piece.PAWN, sq(3, 5)); // d6
            place(pos, false, Piece.PAWN, sq(5, 5)); // f6
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            int[] m = genPseudo(pos, true);
            assertTrue(hasMove(m, sq(4,4), sq(3,5)), "e5xd6");
            assertTrue(hasMove(m, sq(4,4), sq(5,5)), "e5xf6");
        }

        @Test
        @DisplayName("Cannot capture friendly piece diagonally")
        void noFriendlyCapture() {
            var pos = emptyPosition();
            place(pos, true, Piece.PAWN,   sq(4, 4));
            place(pos, true, Piece.KNIGHT, sq(5, 5)); // friendly
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertFalse(hasMove(genPseudo(pos, true), sq(4,4), sq(5,5)));
        }

        @Test
        @DisplayName("a-file pawn does not wrap-capture h-file (left edge)")
        void aFileNoWrap() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(0, 4)); // a5
            place(pos, false, Piece.PAWN, sq(7, 5)); // h6 — wrong-side wrap target
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertFalse(hasMove(genPseudo(pos, true), sq(0,4), sq(7,5)),
                    "a-file pawn must not wrap to h6");
        }

        @Test
        @DisplayName("h-file pawn does not wrap-capture a-file (right edge)")
        void hFileNoWrap() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(7, 4)); // h5
            place(pos, false, Piece.PAWN, sq(0, 5)); // a6 — wrong-side wrap target
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertFalse(hasMove(genPseudo(pos, true), sq(7,4), sq(0,5)),
                    "h-file pawn must not wrap to a6");
        }


        //TODO: implement promotion
        @Test
        @DisplayName("Promotion: exactly 4 moves generated for e7-e8")
        void promotionFourVariants() {
            var pos = oneWhitePawn(4, 6); // e7
            long count = Arrays.stream(genPseudo(pos, true))
                    .filter(m -> Move.from(m) == sq(4,6) && Move.to(m) == sq(4,7))
                    .count();
            assertEquals(4, count, "4 promotion variants expected (Q, R, B, N)");
        }

        //TODO: implement promotion
        @Test
        @DisplayName("Promotion-capture: 4 variants per capturable piece on rank 8")
        void promotionCapture() {
            var pos = oneWhitePawn(4, 6); // e7
            place(pos, false, Piece.ROOK, sq(5, 7)); // f8
            long count = Arrays.stream(genPseudo(pos, true))
                    .filter(m -> Move.from(m) == sq(4,6) && Move.to(m) == sq(5,7))
                    .count();
            assertEquals(4, count, "4 promotion-capture variants expected");
        }
    }

    // =========================================================================
    // 3. Black Pawn
    // =========================================================================

    @Nested
    @DisplayName("Black Pawn")
    class BlackPawnTests {

        PositionEncoder.Position oneBlackPawn(int col, int row) {
            var pos = emptyPosition();
            place(pos, false, Piece.PAWN, sq(col, row));
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            pos.BDoublePawnMove = 1L << sq(col, 6);
            return pos;
        }

        @Test
        @DisplayName("Single push downward from starting rank (row 6)")
        void singlePush() {
            var pos = oneBlackPawn(4, 6); // e7
            assertTrue(hasMove(genPseudo(pos, false), sq(4,6), sq(4,5)), "e7-e6");
        }

        @Test
        @DisplayName("Double push from starting rank")
        void doublePush() {
            var pos = oneBlackPawn(4, 6); // e7
            assertTrue(hasMove(genPseudo(pos, false), sq(4,6), sq(4,4)), "e7-e5");
        }

        @Test
        @DisplayName("No double push when rank-6 square is blocked")
        void noDoublePushRank6Blocked() {
            var pos = oneBlackPawn(4, 6);
            place(pos, true, Piece.PAWN, sq(4, 5)); // e6 blocker
            int[] m = genPseudo(pos, false);
            assertFalse(hasMove(m, sq(4,6), sq(4,5)));
            assertFalse(hasMove(m, sq(4,6), sq(4,4)));
        }

        @Test
        @DisplayName("Black pawn promotion from row 1 to row 0: 4 variants")
        void promotion() {
            var pos = emptyPosition();
            place(pos, false, Piece.PAWN, sq(4, 1)); // e2
            place(pos, true,  Piece.KING, sq(0, 7));
            place(pos, false, Piece.KING, sq(7, 0));
            long count = Arrays.stream(genPseudo(pos, false))
                    .filter(m -> Move.from(m) == sq(4,1) && Move.to(m) == sq(4,0))
                    .count();
            assertEquals(4, count, "4 black promotion variants");
        }

        @Test
        @DisplayName("a-file black pawn does not wrap-capture h-file")
        void aFileNoWrap() {
            var pos = emptyPosition();
            place(pos, false, Piece.PAWN, sq(0, 4)); // a5
            place(pos, true,  Piece.PAWN, sq(7, 3)); // h4 — bad wrap target
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertFalse(hasMove(genPseudo(pos, false), sq(0,4), sq(7,3)));
        }

        @Test
        @DisplayName("h-file black pawn does not wrap-capture a-file")
        void hFileNoWrap() {
            var pos = emptyPosition();
            place(pos, false, Piece.PAWN, sq(7, 4)); // h5
            place(pos, true,  Piece.PAWN, sq(0, 3)); // a4 — bad wrap target
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertFalse(hasMove(genPseudo(pos, false), sq(7,4), sq(0,3)));
        }
    }

    // =========================================================================
    // 4. Knight
    // =========================================================================

    @Nested
    @DisplayName("Knight")
    class KnightTests {

        static final int[][] JUMPS = {
                {2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}
        };

        @Test
        @DisplayName("Center knight (e5) has 8 moves on empty board")
        void centerKnight8Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(4, 4));
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(8, countFrom(genPseudo(pos, true), sq(4,4)));
        }

        @Test
        @DisplayName("Corner a1 knight has 2 moves")
        void cornerA1Knight2Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(0, 0));
            place(pos, true,  Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(5, 5));
            assertEquals(2, countFrom(genPseudo(pos, true), sq(0,0)));
        }

        @Test
        @DisplayName("Corner h8 knight has 2 moves")
        void cornerH8Knight2Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(7, 7));
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(2, 2));
            assertEquals(2, countFrom(genPseudo(pos, true), sq(7,7)));
        }

        @Test
        @DisplayName("Corner a8 knight has 2 moves")
        void cornerA8Knight2Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(0, 7));
            place(pos, true,  Piece.KING, sq(7, 0));
            place(pos, false, Piece.KING, sq(5, 5));
            assertEquals(2, countFrom(genPseudo(pos, true), sq(0,7)));
        }

        @Test
        @DisplayName("Corner h1 knight has 2 moves")
        void cornerH1Knight2Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(7, 0));
            place(pos, true,  Piece.KING, sq(0, 7));
            place(pos, false, Piece.KING, sq(2, 2));
            assertEquals(2, countFrom(genPseudo(pos, true), sq(7,0)));
        }

        @Test
        @DisplayName("a-file knight does not wrap to h-file")
        void aFileNoWrap() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(0, 4)); // a5
            place(pos, true,  Piece.KING, sq(7, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            for (int m : genPseudo(pos, true)) {
                if (Move.from(m) == sq(0,4))
                    assertNotEquals(7, Move.to(m) % 8,
                            "Knight from a-file must not land on h-file via wrap");
            }
        }

        @Test
        @DisplayName("h-file knight does not wrap to a-file")
        void hFileNoWrap() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(7, 4)); // h5
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(2, 2));
            for (int m : genPseudo(pos, true)) {
                if (Move.from(m) == sq(7,4))
                    assertNotEquals(0, Move.to(m) % 8,
                            "Knight from h-file must not land on a-file via wrap");
            }
        }

        @Test
        @DisplayName("Knight surrounded by friendly pieces: 0 moves")
        void allBlockedByFriendlies() {
            var pos = emptyPosition();
            int knightSq = sq(4, 4);
            place(pos, true, Piece.KNIGHT, knightSq);
            for (int[] j : JUMPS) {
                int r = 4 + j[0], c = 4 + j[1];
                if (r >= 0 && r < 8 && c >= 0 && c < 8)
                    place(pos, true, Piece.PAWN, sq(c, r));
            }
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(0, countFrom(genPseudo(pos, true), knightSq));
        }

        @Test
        @DisplayName("Knight surrounded by enemy pieces: 8 capture moves")
        void capturesAllEnemies() {
            var pos = emptyPosition();
            int knightSq = sq(4, 4);
            place(pos, true, Piece.KNIGHT, knightSq);
            for (int[] j : JUMPS) {
                int r = 4 + j[0], c = 4 + j[1];
                if (r >= 0 && r < 8 && c >= 0 && c < 8)
                    place(pos, false, Piece.PAWN, sq(c, r));
            }
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(8, countFrom(genPseudo(pos, true), knightSq));
        }

        @Test
        @DisplayName("b-file knight (b1) has 3 moves")
        void bFileKnightHas3Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KNIGHT, sq(1, 0)); // b1
            place(pos, true,  Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(5, 5));
            // b1 knight can go to a3, c3, d2 — exactly 3
            assertEquals(3, countFrom(genPseudo(pos, true), sq(1,0)));
        }
    }

    // =========================================================================
    // 5. Bishop
    // =========================================================================

    @Nested
    @DisplayName("Bishop")
    class BishopTests {

        @Test
        @DisplayName("Center bishop (e5) has 13 moves on empty board")
        void centerBishop13Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.BISHOP, sq(4, 4));
            place(pos, true,  Piece.KING, sq(1, 0));    // move king out of way of
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(13, countFrom(genPseudo(pos, true), sq(4,4)));
        }

        @Test
        @DisplayName("Corner a1 bishop has 7 moves")
        void cornerA1Bishop7Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.BISHOP, sq(0, 0));
            place(pos, true,  Piece.KING, sq(6, 7));    // move king out of way
            place(pos, false, Piece.KING, sq(5, 6));
            assertEquals(7, countFrom(genPseudo(pos, true), sq(0,0)));
        }

        @Test
        @DisplayName("Bishop blocked by friendly: cannot land on or jump over")
        void blockedByFriendly() {
            var pos = emptyPosition();
            place(pos, true, Piece.BISHOP, sq(0, 0));  // a1
            place(pos, true, Piece.PAWN,   sq(2, 2));  // c3 blocks
            place(pos, true,  Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(5, 6));
            int[] m = genPseudo(pos, true);
            assertTrue(hasMove(m,  sq(0,0), sq(1,1)), "b2 reachable");
            assertFalse(hasMove(m, sq(0,0), sq(2,2)), "c3 blocked (friendly)");
            assertFalse(hasMove(m, sq(0,0), sq(3,3)), "d4 behind blocker");
        }

        @Test
        @DisplayName("Bishop captures enemy and cannot continue past it")
        void captureAndStop() {
            var pos = emptyPosition();
            place(pos, true,  Piece.BISHOP, sq(0, 0)); // a1
            place(pos, false, Piece.PAWN,   sq(2, 2)); // c3 enemy
            place(pos, true,  Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(5, 6));
            int[] m = genPseudo(pos, true);
            assertTrue(hasMove(m,  sq(0,0), sq(2,2)), "a1xc3");
            assertFalse(hasMove(m, sq(0,0), sq(3,3)), "cannot continue past capture");
        }

        @Test
        @DisplayName("Bishop from h1 does not wrap to h-file on upper diagonal")
        void hFileBishopNoWrap() {
            var pos = emptyPosition();
            place(pos, true, Piece.BISHOP, sq(7, 0)); // h1
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            for (int m : genPseudo(pos, true))
                if (Move.from(m) == sq(7,0))
                    assertNotEquals(7, Move.to(m) % 8, "h1 bishop diagonal must not re-land on h-file");
        }

        @Test
        @DisplayName("Bishop from a8 does not wrap to a-file on lower diagonal")
        void aFileBishopNoWrap() {
            var pos = emptyPosition();
            place(pos, true, Piece.BISHOP, sq(0, 7)); // a8
            place(pos, true,  Piece.KING, sq(7, 0));
            place(pos, false, Piece.KING, sq(5, 5));
            for (int m : genPseudo(pos, true))
                if (Move.from(m) == sq(0,7))
                    assertNotEquals(0, Move.to(m) % 8, "a8 bishop diagonal must not re-land on a-file");
        }
    }

    // =========================================================================
    // 6. Rook
    // =========================================================================

    @Nested
    @DisplayName("Rook")
    class RookTests {

        @Test
        @DisplayName("Center rook (e5) has 14 moves on empty board")
        void centerRook14Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.ROOK, sq(4, 4));
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(14, countFrom(genPseudo(pos, true), sq(4,4)));
        }

        @Test
        @DisplayName("Corner a1 rook has 14 moves on empty board")
        void cornerA1Rook14Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.ROOK, sq(0, 0));
            place(pos, true,  Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(5, 6));
            assertEquals(14, countFrom(genPseudo(pos, true), sq(0,0)));
        }

        @Test
        @DisplayName("Rook blocked by friendly: cannot land on or jump over")
        void blockedByFriendly() {
            var pos = emptyPosition();
            place(pos, true, Piece.ROOK,   sq(0, 0)); // a1
            place(pos, true, Piece.KNIGHT, sq(0, 3)); // a4 blocker
            place(pos, true,  Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(5, 6));
            int[] m = genPseudo(pos, true);
            assertTrue(hasMove(m,  sq(0,0), sq(0,1)));
            assertTrue(hasMove(m,  sq(0,0), sq(0,2)));
            assertFalse(hasMove(m, sq(0,0), sq(0,3)), "blocked by friendly");
            assertFalse(hasMove(m, sq(0,0), sq(0,4)), "cannot jump over");
        }

        @Test
        @DisplayName("Rook captures enemy and stops")
        void captureAndStop() {
            var pos = emptyPosition();
            place(pos, true,  Piece.ROOK, sq(0, 0));
            place(pos, false, Piece.PAWN, sq(0, 3)); // a4 enemy
            place(pos, true,  Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(5, 6));
            int[] m = genPseudo(pos, true);
            assertTrue(hasMove(m,  sq(0,0), sq(0,3)), "a1xa4");
            assertFalse(hasMove(m, sq(0,0), sq(0,4)), "cannot continue after capture");
        }

        @Test
        @DisplayName("h-file rook does not wrap horizontally to a-file")
        void hFileNoWrap() {
            var pos = emptyPosition();
            place(pos, true, Piece.ROOK, sq(7, 4)); // h5
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(2, 2));
            for (int m : genPseudo(pos, true)) {
                if (Move.from(m) == sq(7,4)) {
                    int destRow = Move.to(m) / 8;
                    if (destRow == 4) // same rank: moves go left only
                        assertTrue(Move.to(m) % 8 < 7,
                                "h-file rook must move left, not right-wrap");
                }
            }
        }

        @Test
        @DisplayName("a-file rook does not wrap horizontally to h-file")
        void aFileNoWrap() {
            var pos = emptyPosition();
            place(pos, true, Piece.ROOK, sq(0, 4)); // a5
            place(pos, true,  Piece.KING, sq(7, 0));
            place(pos, false, Piece.KING, sq(5, 7));
            for (int m : genPseudo(pos, true)) {
                if (Move.from(m) == sq(0,4)) {
                    int destRow = Move.to(m) / 8;
                    if (destRow == 4)
                        assertTrue(Move.to(m) % 8 > 0,
                                "a-file rook must move right, not left-wrap");
                }
            }
        }
    }

    // =========================================================================
    // 7. Queen
    // =========================================================================

    @Nested
    @DisplayName("Queen")
    class QueenTests {

        @Test
        @DisplayName("Center queen (e5) has 27 moves on empty board")
        void centerQueen27Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.QUEEN, sq(4, 4));
            place(pos, true,  Piece.KING, sq(1, 0));        // move King
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(27, countFrom(genPseudo(pos, true), sq(4,4)));
        }

        @Test
        @DisplayName("Corner a1 queen has 21 moves on empty board")
        void cornerA1Queen21Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.QUEEN, sq(0, 0));
            place(pos, true,  Piece.KING, sq(6, 7));        // move King
            place(pos, false, Piece.KING, sq(5, 6));
            assertEquals(21, countFrom(genPseudo(pos, true), sq(0,0)));
        }

        @Test
        @DisplayName("Queen blocked by friendly in all 8 directions: 0 moves")
        void blockedByFriendliesAllDirections() {
            var pos = emptyPosition();
            int qSq = sq(4, 4);
            place(pos, true, Piece.QUEEN, qSq);
            // Place friendly pieces on all adjacent squares
            for (int dr = -1; dr <= 1; dr++)
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    place(pos, true, Piece.PAWN, sq(4+dc, 4+dr));
                }
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(0, countFrom(genPseudo(pos, true), qSq));
        }

        @Test
        @DisplayName("a5 queen does not wrap horizontally to h-file")
        void noHorizontalWrap() {
            var pos = emptyPosition();
            place(pos, true, Piece.QUEEN, sq(0, 4)); // a5
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            for (int m : genPseudo(pos, true))
                if (Move.from(m) == sq(0,4) && Move.to(m)/8 == 4)
                    assertTrue(Move.to(m) % 8 > 0, "a5 queen must not wrap left to h-file");
        }
    }

    // =========================================================================
    // 8. King
    // =========================================================================

    @Nested
    @DisplayName("King")
    class KingTests {

        @Test
        @DisplayName("Center king (e5) has 8 moves")
        void centerKing8Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING, sq(4, 4));
            place(pos, false, Piece.KING, sq(0, 7));
            assertEquals(8, countFrom(genPseudo(pos, true), sq(4,4)));
        }

        @Test
        @DisplayName("Corner a1 king has 3 moves")
        void cornerA1King3Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertEquals(3, countFrom(genPseudo(pos, true), sq(0,0)));
        }

        @Test
        @DisplayName("Corner h8 king has 3 moves")
        void cornerH8King3Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING, sq(7, 7));
            place(pos, false, Piece.KING, sq(0, 0));
            assertEquals(3, countFrom(genPseudo(pos, true), sq(7,7)));
        }

        @Test
        @DisplayName("Corner h1 king has 3 moves")
        void cornerH1King3Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING, sq(7, 0));
            place(pos, false, Piece.KING, sq(0, 7));
            assertEquals(3, countFrom(genPseudo(pos, true), sq(7,0)));
        }

        @Test
        @DisplayName("Corner a8 king has 3 moves")
        void cornerA8King3Moves() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING, sq(0, 7));
            place(pos, false, Piece.KING, sq(7, 0));
            assertEquals(3, countFrom(genPseudo(pos, true), sq(0,7)));
        }

        @Test
        @DisplayName("a-file king does not wrap to h-file")
        void aFileKingNoWrap() {
            var pos = emptyPosition();
            place(pos, true,  Piece.KING, sq(0, 4)); // a5
            place(pos, false, Piece.KING, sq(7, 4)); // far away
            for (int m : genPseudo(pos, true))
                if (Move.from(m) == sq(0,4))
                    assertNotEquals(7, Move.to(m) % 8, "a-file king must not wrap to h-file");
        }

        @Test
        @DisplayName("h-file king does not wrap to a-file")
        void hFileKingNoWrap() {
            var pos = emptyPosition();
            place(pos, true,  Piece.KING, sq(7, 4)); // h5
            place(pos, false, Piece.KING, sq(0, 0));
            for (int m : genPseudo(pos, true))
                if (Move.from(m) == sq(7,4))
                    assertNotEquals(0, Move.to(m) % 8, "h-file king must not wrap to a-file");
        }

        @Test
        @DisplayName("King cannot capture friendly piece")
        void noFriendlyCapture() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING,   sq(4, 4));
            place(pos, true, Piece.KNIGHT, sq(5, 5)); // friendly
            place(pos, false, Piece.KING, sq(0, 0));
            assertFalse(hasMove(genPseudo(pos, true), sq(4,4), sq(5,5)));
        }

        @Test
        @DisplayName("Legal filter: king cannot move into check (pawn attack)")
        void kingCannotMoveIntoCheckByPawn() {
            var pos = emptyPosition();
            place(pos, true,  Piece.KING, sq(4, 0)); // e1
            place(pos, false, Piece.PAWN, sq(3, 2)); // d3 covers c2 and e2
            place(pos, false, Piece.KING, sq(7, 7));
            int[] legal = genLegal(pos, true);
            assertFalse(hasMove(legal, sq(4,0), sq(4,1)), "e2 is attacked by d3 pawn");
            assertFalse(hasMove(legal, sq(4,0), sq(2,1)), "c2 is attacked by d3 pawn");
        }
    }

    // =========================================================================
    // 9. Castling
    // =========================================================================

    @Nested
    @DisplayName("Castling")
    class CastlingTests {

        PositionEncoder.Position whiteKSReady() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING, sq(4, 0)); // e1
            place(pos, true, Piece.ROOK, sq(7, 0)); // h1
            place(pos, false, Piece.KING, sq(4, 7));
            pos.WKingCastlePerm = true;
            pos.WKingCastle     = true;
            return pos;
        }

        PositionEncoder.Position whiteQSReady() {
            var pos = emptyPosition();
            place(pos, true, Piece.KING, sq(4, 0)); // e1
            place(pos, true, Piece.ROOK, sq(0, 0)); // a1
            place(pos, false, Piece.KING, sq(4, 7));
            pos.WQueenCastlePerm = true;
            pos.WQueenCastle     = true;
            return pos;
        }

        @Test
        @DisplayName("White O-O generated when path is clear and rights are set")
        void whiteKingSideCastleGenerated() {
            int[] m = genPseudo(whiteKSReady(), true);
            assertTrue(Arrays.stream(m).anyMatch(mv ->
                            Move.from(mv) == sq(4,0) && Move.to(mv) == sq(6,0)
                                    && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_K),
                    "White O-O must be generated");
        }

        @Test
        @DisplayName("White O-O-O generated when path is clear and rights are set")
        void whiteQueenSideCastleGenerated() {
            int[] m = genPseudo(whiteQSReady(), true);
            assertTrue(Arrays.stream(m).anyMatch(mv ->
                            Move.from(mv) == sq(4,0) && Move.to(mv) == sq(2,0)
                                    && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_Q),
                    "White O-O-O must be generated");
        }

        @Test
        @DisplayName("White O-O NOT generated when f1 is occupied")
        void whiteKSBlockedF1() {
            var pos = whiteKSReady();
            place(pos, true, Piece.BISHOP, sq(5, 0)); // f1
            assertFalse(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(4,0) && Move.to(mv) == sq(6,0)
                            && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_K));
        }

        @Test
        @DisplayName("White O-O NOT generated when g1 is occupied")
        void whiteKSBlockedG1() {
            var pos = whiteKSReady();
            place(pos, true, Piece.KNIGHT, sq(6, 0)); // g1
            assertFalse(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(4,0) && Move.to(mv) == sq(6,0)
                            && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_K));
        }

        @Test
        @DisplayName("White O-O-O NOT generated when d1 is occupied")
        void whiteQSBlockedD1() {
            var pos = whiteQSReady();
            place(pos, true, Piece.QUEEN, sq(3, 0)); // d1
            assertFalse(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(4,0) && Move.to(mv) == sq(2,0)
                            && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_Q));
        }

        @Test
        @DisplayName("White O-O NOT generated without castling rights")
        void whiteKSNoRights() {
            var pos = whiteKSReady();
            pos.WKingCastle = false;
            assertFalse(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(4,0) && Move.to(mv) == sq(6,0)
                            && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_K));
        }

        @Test
        @DisplayName("White O-O NOT generated when king passes through check (f1 attacked)")
        void whiteKSCastleThroughCheck() {
            var pos = whiteKSReady();
            place(pos, false, Piece.ROOK, sq(5, 7)); // f8 covers f1
            assertFalse(Arrays.stream(genLegal(pos, true)).anyMatch(mv ->
                            Move.from(mv) == sq(4,0) && Move.to(mv) == sq(6,0)
                                    && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_K),
                    "King cannot castle through check on f1");
        }

        @Test
        @DisplayName("White O-O NOT generated when king is in check")
        void whiteKSCastleWhileInCheck() {
            var pos = whiteKSReady();
            place(pos, false, Piece.ROOK, sq(4, 7)); // e8 covers e1
            assertFalse(Arrays.stream(genLegal(pos, true)).anyMatch(mv ->
                            Move.from(mv) == sq(4,0) && Move.to(mv) == sq(6,0)
                                    && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_K),
                    "King cannot castle while in check");
        }

        @Test
        @DisplayName("Black O-O generated symmetrically")
        void blackKingSideCastle() {
            var pos = emptyPosition();
            place(pos, false, Piece.KING, sq(4, 7)); // e8
            place(pos, false, Piece.ROOK, sq(7, 7)); // h8
            place(pos, true,  Piece.KING, sq(4, 0));
            pos.BKingCastlePerm = true;
            pos.BKingCastle     = true;
            assertTrue(Arrays.stream(genPseudo(pos, false)).anyMatch(mv ->
                    Move.from(mv) == sq(4,7) && Move.to(mv) == sq(6,7)
                            && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_K));
        }

        @Test
        @DisplayName("Black O-O-O generated symmetrically")
        void blackQueenSideCastle() {
            var pos = emptyPosition();
            place(pos, false, Piece.KING, sq(4, 7)); // e8
            place(pos, false, Piece.ROOK, sq(0, 7)); // a8
            place(pos, true,  Piece.KING, sq(4, 0));
            pos.BQueenCastlePerm = true;
            pos.BQueenCastle     = true;
            assertTrue(Arrays.stream(genPseudo(pos, false)).anyMatch(mv ->
                    Move.from(mv) == sq(4,7) && Move.to(mv) == sq(2,7)
                            && Move.baseFlag(Move.flags(mv)) == Move.FLAG_CASTLE_Q));
        }
    }

    // =========================================================================
    // 10. En Passant
    // =========================================================================

    @Nested
    @DisplayName("En Passant")
    class EnPassantTests {

        @Test
        @DisplayName("White pawn captures en passant to the left (e5xd6)")
        void whiteEPLeft() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(4, 4)); // e5
            place(pos, false, Piece.PAWN, sq(3, 4)); // d5
            pos.enPassantSquare = sq(3, 5);           // d6
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertTrue(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(4,4) && Move.to(mv) == sq(3,5)
                            && Move.flags(mv) == Move.FLAG_EN_PASSANT_CAPTURE));
        }

        @Test
        @DisplayName("White pawn captures en passant to the right (e5xf6)")
        void whiteEPRight() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(4, 4)); // e5
            place(pos, false, Piece.PAWN, sq(5, 4)); // f5
            pos.enPassantSquare = sq(5, 5);           // f6
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertTrue(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(4,4) && Move.to(mv) == sq(5,5)
                            && Move.flags(mv) == Move.FLAG_EN_PASSANT_CAPTURE));
        }

        @Test
        @DisplayName("No en passant generated when enPassantSquare == -1")
        void noEPWhenSquareNegative() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(4, 4));
            place(pos, false, Piece.PAWN, sq(3, 4));
            pos.enPassantSquare = -1;
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertFalse(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.flags(mv) == Move.FLAG_EN_PASSANT_CAPTURE));
        }

        @Test
        @DisplayName("Black pawn captures en passant (e4xf3)")
        void blackEP() {
            var pos = emptyPosition();
            place(pos, false, Piece.PAWN, sq(4, 3)); // e4
            place(pos, true,  Piece.PAWN, sq(5, 3)); // f4
            pos.enPassantSquare = sq(5, 2);           // f3
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertTrue(Arrays.stream(genPseudo(pos, false)).anyMatch(mv ->
                    Move.from(mv) == sq(4,3) && Move.to(mv) == sq(5,2)
                            && Move.flags(mv) == Move.FLAG_EN_PASSANT_CAPTURE));
        }

        @Test
        @DisplayName("b5 pawn can en passant capture a6 (a-file edge, no wrap confusion)")
        void aFileEdgeEP() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(1, 4)); // b5
            place(pos, false, Piece.PAWN, sq(0, 4)); // a5
            pos.enPassantSquare = sq(0, 5);           // a6
            place(pos, true,  Piece.KING, sq(7, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertTrue(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(1,4) && Move.to(mv) == sq(0,5)
                            && Move.flags(mv) == Move.FLAG_EN_PASSANT_CAPTURE));
        }

        @Test
        @DisplayName("g5 pawn can en passant capture h6 (h-file edge, no wrap confusion)")
        void hFileEdgeEP() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(6, 4)); // g5
            place(pos, false, Piece.PAWN, sq(7, 4)); // h5
            pos.enPassantSquare = sq(7, 5);           // h6
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            assertTrue(Arrays.stream(genPseudo(pos, true)).anyMatch(mv ->
                    Move.from(mv) == sq(6,4) && Move.to(mv) == sq(7,5)
                            && Move.flags(mv) == Move.FLAG_EN_PASSANT_CAPTURE));
        }
    }

    // =========================================================================
    // 11. Promotion flags
    // =========================================================================

    @Nested
    @DisplayName("Promotion flags")
    class PromotionFlagTests {

        @Test
        @DisplayName("Four distinct flag values for e7-e8 promotion")
        void fourDistinctFlagsForPromotion() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(4, 6)); // e7
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(7, 7));
            Set<Integer> flags = Arrays.stream(genPseudo(pos, true))
                    .filter(m -> Move.from(m) == sq(4,6) && Move.to(m) == sq(4,7))
                    .mapToObj(Move::flags)
                    .collect(Collectors.toSet());
            assertEquals(4, flags.size(),
                    "Must have 4 distinct flags for Q/R/B/N promotion");
        }

        @Test
        @DisplayName("Four distinct flag values for promotion-capture f7xg8")
        void fourDistinctFlagsForPromotionCapture() {
            var pos = emptyPosition();
            place(pos, true,  Piece.PAWN, sq(5, 6)); // f7
            place(pos, false, Piece.ROOK, sq(6, 7)); // g8
            place(pos, true,  Piece.KING, sq(0, 0));
            place(pos, false, Piece.KING, sq(4, 7));
            Set<Integer> flags = Arrays.stream(genPseudo(pos, true))
                    .filter(m -> Move.from(m) == sq(5,6) && Move.to(m) == sq(6,7))
                    .mapToObj(Move::flags)
                    .collect(Collectors.toSet());
            assertEquals(4, flags.size(),
                    "Must have 4 distinct flags for promotion-capture Q/R/B/N");
        }
    }

    // =========================================================================
    // 12. Check / legal-move filter
    // =========================================================================

    @Nested
    @DisplayName("Legal move filter")
    class LegalMoveFilterTests {

        @Test
        @DisplayName("Pinned rook cannot move off pin ray")
        void pinnedRookStaysOnRay() {
            var pos = emptyPosition();
            // e1 king, e4 rook pinned by e8 black rook
            place(pos, true,  Piece.KING, sq(4, 0));
            place(pos, true,  Piece.ROOK, sq(4, 3)); // e4
            place(pos, false, Piece.ROOK, sq(4, 7)); // e8
            place(pos, false, Piece.KING, sq(0, 7));
            int[] legal = genLegal(pos, true);
            // The pinned rook may only move along e-file (col 4)
            boolean offRay = Arrays.stream(legal)
                    .anyMatch(m -> Move.from(m) == sq(4,3) && Move.to(m) % 8 != 4);
            assertFalse(offRay, "Pinned rook must stay on the e-file");
        }

        @Test
        @DisplayName("Every legal move leaves the king out of check")
        void allLegalMovesLeaveKingSafe() {
            var pos = emptyPosition();
            // King in check from queen on e8
            place(pos, true,  Piece.KING,  sq(4, 0));
            place(pos, false, Piece.QUEEN, sq(4, 7));
            place(pos, false, Piece.KING,  sq(0, 7));
            for (int m : genLegal(pos, true)) {
                var copy = deepCopy(pos);
                copy.makeMove(m);
                assertFalse(copy.checkChess(true),
                        "After legal move white king must not be in check");
            }
        }

        @Test
        @DisplayName("Legal move count <= pseudo-move count")
        void legalLteqPseudo() {
            var pos = emptyPosition();
            place(pos, true,  Piece.KING,  sq(4, 0));
            place(pos, false, Piece.QUEEN, sq(4, 7));
            place(pos, false, Piece.KING,  sq(0, 7));
            assertTrue(genLegal(pos, true).length <= genPseudo(pos, true).length);
        }

        @Test
        @DisplayName("Stalemate: 0 legal moves")
        void stalemate() {
            // Classic: white king a1, black queen b3, black king c2
            var pos = emptyPosition();
            place(pos, true,  Piece.KING,  sq(0, 0)); // a1
            place(pos, false, Piece.QUEEN, sq(1, 2)); // b3
            place(pos, false, Piece.KING,  sq(2, 1)); // c2
            assertEquals(0, genLegal(pos, true).length, "Stalemate = 0 legal moves");
        }

        @Test
        @DisplayName("Back-rank checkmate: 0 legal moves")
        void backRankCheckmate() {
            // h1 king, black rooks on a1 and a2
            var pos = emptyPosition();
            place(pos, true,  Piece.KING, sq(7, 0)); // h1
            place(pos, false, Piece.ROOK, sq(0, 0)); // a1
            place(pos, false, Piece.ROOK, sq(0, 1)); // a2
            place(pos, false, Piece.KING, sq(5, 7));
            assertEquals(0, genLegal(pos, true).length, "Checkmate = 0 legal moves");
        }
    }

    // =========================================================================
    // 13. Full-position integration + perft
    // =========================================================================

    @Nested
    @DisplayName("Integration / Perft")
    class IntegrationTests {

        /** Standard chess starting position. */
        static PositionEncoder.Position startPos() {
            var pos = emptyPosition();
            // White back rank
            place(pos, true, Piece.ROOK,   sq(0,0)); place(pos, true, Piece.KNIGHT, sq(1,0));
            place(pos, true, Piece.BISHOP, sq(2,0)); place(pos, true, Piece.QUEEN,  sq(3,0));
            place(pos, true, Piece.KING,   sq(4,0)); place(pos, true, Piece.BISHOP, sq(5,0));
            place(pos, true, Piece.KNIGHT, sq(6,0)); place(pos, true, Piece.ROOK,   sq(7,0));
            for (int c = 0; c < 8; c++) place(pos, true, Piece.PAWN, sq(c,1));
            // Black back rank
            place(pos, false, Piece.ROOK,   sq(0,7)); place(pos, false, Piece.KNIGHT, sq(1,7));
            place(pos, false, Piece.BISHOP, sq(2,7)); place(pos, false, Piece.QUEEN,  sq(3,7));
            place(pos, false, Piece.KING,   sq(4,7)); place(pos, false, Piece.BISHOP, sq(5,7));
            place(pos, false, Piece.KNIGHT, sq(6,7)); place(pos, false, Piece.ROOK,   sq(7,7));
            for (int c = 0; c < 8; c++) place(pos, false, Piece.PAWN, sq(c,6));
            // All castling rights
            pos.WKingCastlePerm = true;  pos.WKingCastle  = true;
            pos.WQueenCastlePerm = true; pos.WQueenCastle = true;
            pos.BKingCastlePerm = true;  pos.BKingCastle  = true;
            pos.BQueenCastlePerm = true; pos.BQueenCastle = true;
            return pos;
        }

        static long perft(PositionEncoder.Position pos, int depth, boolean white) {
            if (depth == 0) return 1;
            int[] buf = new int[256];
            int count = MoveGen.generateMoves(pos, 0, white, buf);
            long nodes = 0;
            for (int i = 0; i < count; i++) {
                int undo = pos.makeMove(buf[i]);
                nodes += perft(pos, depth - 1, !white);
                pos.unmakeMove(undo);
            }
            return nodes;
        }

        @Test
        @DisplayName("Starting position: white has exactly 20 legal moves")
        void startPosWhite20() {
            assertEquals(20, genLegal(PositionEncoder.Position.StartingPosition(), true).length);
        }

        @Test
        @DisplayName("Starting position: black has exactly 20 legal moves")
        void startPosBlack20() {
            assertEquals(20, genLegal(PositionEncoder.Position.StartingPosition(), false).length);
        }

        @Test
        @DisplayName("Perft depth 1 = 20")
        void perftD1() {
            assertEquals(20, perft(PositionEncoder.Position.StartingPosition(), 1, true));
        }

        @Test
        @DisplayName("Perft depth 2 = 400")
        void perftD2() {
            assertEquals(400, perft(PositionEncoder.Position.StartingPosition(), 2, true));
        }

        // Depth 3 (8902) and 4 (197281) — enable once the engine is fast enough.
        // @Test @DisplayName("Perft depth 3 = 8902")
        // void perftD3() { assertEquals(8902, perft(startPos(), 3, true)); }
        // @Test @DisplayName("Perft depth 4 = 197281")
        // void perftD4() { assertEquals(197281, perft(startPos(), 4, true)); }

        @Test
        @DisplayName("No move has from == to in starting position")
        void noSelfMoves() {
            for (int m : genPseudo(startPos(), true))
                assertNotEquals(Move.from(m), Move.to(m),
                        "from == to in move: " + Integer.toBinaryString(m));
        }

        @Test
        @DisplayName("All move squares are within [0, 63]")
        void squaresInBounds() {
            for (int m : genPseudo(startPos(), true)) {
                assertTrue(Move.from(m) >= 0 && Move.from(m) < 64);
                assertTrue(Move.to(m)   >= 0 && Move.to(m)   < 64);
            }
        }

        @Test
        @DisplayName("Pseudo-move count >= legal move count in starting position")
        void pseudoGteqLegal() {
            var pos = startPos();
            assertTrue(genPseudo(pos, true).length >= genLegal(pos, true).length);
        }

        @Test
        @DisplayName("Offset parameter: moves are appended at the correct index")
        void offsetParameterRespected() {
            var pos = startPos();
            int[] buf = new int[512];
            int offset = 50;
            int newOffset = MoveGen.generatePseudoMoves(pos, offset, true, buf);
            assertTrue(newOffset > offset, "New offset must be greater than start offset");
            // Entries before offset must be zero (untouched)
            for (int i = 0; i < offset; i++)
                assertEquals(0, buf[i], "Slot before offset must be untouched");
        }
    }
}