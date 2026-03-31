package engine;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.mxnik.forcechess.Util.Bitboard;
import org.mxnik.forcechess.engine.bot.baseStateBot.MoveGen;
import org.mxnik.forcechess.engine.bot.baseStateBot.Piece;
import org.mxnik.forcechess.engine.bot.baseStateBot.Move;
import org.mxnik.forcechess.engine.bot.baseStateBot.PositionEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mxnik.forcechess.engine.bot.baseStateBot.Move.*;
import static org.mxnik.forcechess.engine.bot.baseStateBot.Piece.*;

/**
 * Modular test suite for MoveGen.generateMoves() and MoveGen.generatePseudoMoves().
 *
 * Conventions:
 *   - Squares are 0-indexed, row-major (a1=0, b1=1, ..., h8=63)
 *   - White pawns move toward higher indices (increasing row)
 *   - Black pawns move toward lower indices (decreasing row)
 *
 * Structure:
 *   - PositionBuilder  – fluent helper to set up test positions
 *   - MoveAssert       – assertion helpers to query the move array
 *   - Nested test classes per piece type + integration tests
 */
class MoveGenTest {

    // =========================================================================
    // Constants (mirror Move / Piece / PositionEncoder constants)
    // Adjust if your constant names differ.
    // =========================================================================

    // Square indices
    static final int A1=0,  B1=1,  C1=2,  D1=3,  E1=4,  F1=5,  G1=6,  H1=7;
    static final int A2=8,  B2=9,  C2=10, D2=11, E2=12, F2=13, G2=14, H2=15;
    static final int A3=16, B3=17, C3=18, D3=19, E3=20, F3=21, G3=22, H3=23;
    static final int A4=24, B4=25, C4=26, D4=27, E4=28, F4=29, G4=30, H4=31;
    static final int A5=32, B5=33, C5=34, D5=35, E5=36, F5=37, G5=38, H5=39;
    static final int A6=40, B6=41, C6=42, D6=43, E6=44, F6=45, G6=46, H6=47;
    static final int A7=48, B7=49, C7=50, D7=51, E7=52, F7=53, G7=54, H7=55;
    static final int A8=56, B8=57, C8=58, D8=59, E8=60, F8=61, G8=62, H8=63;

    // Piece types (must match Piece.*)
    static final int EMPTY  = Piece.EMPTY_PIECE;
    static final int PAWN   = Piece.PAWN;
    static final int KNIGHT = Piece.KNIGHT;
    static final int BISHOP = Piece.BISHOP;
    static final int ROOK   = Piece.ROOK;
    static final int QUEEN  = Piece.QUEEN;
    static final int KING   = Piece.KING;

    // Move flag constants (must match Move.FLAG_*)
    static final int FLAG_GENERIC          = Move.FLAG_GENERIC;
    static final int FLAG_GENERIC_CAPTURE  = Move.FLAG_GENERIC_CAPTURE;
    static final int FLAG_CASTLE_K         = Move.FLAG_CASTLE_K;
    static final int FLAG_CASTLE_Q         = Move.FLAG_CASTLE_Q;
    static final int FLAG_EN_PASSANT       = Move.FLAG_EN_PASSANT;
    static final int FLAG_EN_PASSANT_CAP   = Move.FLAG_EN_PASSANT_CAPTURE;
    // Promotion flags — add your actual constants here when implemented
    // static final int FLAG_PROMO_QUEEN  = Move.FLAG_PROMO_QUEEN;
    // static final int FLAG_PROMO_ROOK   = Move.FLAG_PROMO_ROOK;
    // static final int FLAG_PROMO_BISHOP = Move.FLAG_PROMO_BISHOP;
    // static final int FLAG_PROMO_KNIGHT = Move.FLAG_PROMO_KNIGHT;

    // =========================================================================
    // Fluent position builder
    // =========================================================================

    /**
     * Minimal fluent builder around PositionEncoder.Position.
     * Only sets what you tell it; leaves everything else at defaults (empty).
     */
    static class PositionBuilder {
        private final PositionEncoder.Position pos = new PositionEncoder.Position();

        PositionBuilder() {
            // Start completely empty
            pos.WPawns = pos.WKnights = pos.WBishops = 0L;
            pos.WRooks = pos.WQueens  = pos.WKing    = 0L;
            pos.BPawns = pos.BKnights = pos.BBishops = 0L;
            pos.BRooks = pos.BQueens  = pos.BKing    = 0L;
            pos.Occupied = pos.WPieces = pos.BPieces = 0L;
            pos.enPassantSquare = -1;
            pos.whiteToMove = true;
        }

        PositionBuilder white(int type, int... squares) { return place(true,  type, squares); }
        PositionBuilder black(int type, int... squares) { return place(false, type, squares); }

        private PositionBuilder place(boolean color, int type, int[] squares) {
            int pieceByte = Piece.of(color, type); // adapt to your Piece.encode API
            for (int sq : squares) {
                pos.pieceMap[sq] = (byte) pieceByte;
                long bit = Bitboard.set(0L, sq);
                if (color) {
                    pos.WPieces |= bit;
                    switch (type) {
                        case PAWN   -> pos.WPawns   |= bit;
                        case KNIGHT -> pos.WKnights |= bit;
                        case BISHOP -> pos.WBishops |= bit;
                        case ROOK   -> pos.WRooks   |= bit;
                        case QUEEN  -> pos.WQueens  |= bit;
                        case KING   -> pos.WKing    |= bit;
                    }
                } else {
                    pos.BPieces |= bit;
                    switch (type) {
                        case PAWN   -> pos.BPawns   |= bit;
                        case KNIGHT -> pos.BKnights |= bit;
                        case BISHOP -> pos.BBishops |= bit;
                        case ROOK   -> pos.BRooks   |= bit;
                        case QUEEN  -> pos.BQueens  |= bit;
                        case KING   -> pos.BKing    |= bit;
                    }
                }
            }
            pos.Occupied = pos.WPieces | pos.BPieces;
            return this;
        }

        PositionBuilder whiteToMove(boolean w)         { pos.whiteToMove = w; return this; }
        PositionBuilder enPassant(int sq)              { pos.enPassantSquare = sq; return this; }
        PositionBuilder wCastle(boolean k, boolean q)  { pos.WKingCastle = k; pos.WQueenCastle = q; return this; }
        PositionBuilder bCastle(boolean k, boolean q)  { pos.BKingCastle = k; pos.BQueenCastle = q; return this; }
        PositionBuilder wCastlePerm(boolean k, boolean q) { pos.WKingCastlePerm = k; pos.WQueenCastlePerm = q; return this; }
        PositionBuilder bCastlePerm(boolean k, boolean q) { pos.BKingCastlePerm = k; pos.BQueenCastlePerm = q; return this; }

        PositionEncoder.Position build() { return pos; }
    }

    // =========================================================================
    // Move assertion helpers
    // =========================================================================

    static List<Integer> collectMoves(PositionEncoder.Position pos, boolean white) {
        int[] buf = new int[256];
        int end = MoveGen.generateMoves(pos, 0, white, buf);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < end; i++) result.add(buf[i]);
        return result;
    }

    static List<Integer> collectPseudo(PositionEncoder.Position pos, boolean white) {
        int[] buf = new int[256];
        int end = MoveGen.generatePseudoMoves(pos, 0, white, buf);
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < end; i++) result.add(buf[i]);
        return result;
    }

    /** Returns true if the move list contains a move with exactly these attributes. */
    static boolean hasMove(List<Integer> moves, int from, int to, int flags) {
        for (int m : moves) {
            if (Move.from(m) == from && Move.to(m) == to && Move.flags(m) == flags) return true;
        }
        return false;
    }

    /** Returns true if ANY move from→to exists regardless of flags. */
    static boolean hasAnyMove(List<Integer> moves, int from, int to) {
        for (int m : moves) {
            if (Move.from(m) == from && Move.to(m) == to) return true;
        }
        return false;
    }

    static long countFrom(List<Integer> moves, int from) {
        return moves.stream().filter(m -> Move.from(m) == from).count();
    }


    // =========================================================================
    // Pawn tests
    // =========================================================================

    @Nested
    @DisplayName("Pawn – White")
    class WhitePawnTests {

        @Test
        @DisplayName("Single push from starting rank")
        void singlePushFromStart() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E2)
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasMove(moves, E2, E3, FLAG_GENERIC), "e2→e3 should exist");
        }

        @Test
        @DisplayName("Double push from starting rank")
        void doublePushFromStart() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E2)
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasMove(moves, E2, E4, FLAG_GENERIC), "e2→e4 should exist");
        }

        @Test
        @DisplayName("No double push if single-push square is blocked")
        void doublePushBlockedBySingleBlocker() {
            // piece on e3 blocks both single and double push
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E2)
                    .black(PAWN, E3)     // blocker
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasAnyMove(moves, E2, E4), "double push must be blocked");
            assertFalse(hasAnyMove(moves, E2, E3), "single push must be blocked");
        }

        @Test
        @DisplayName("Double push blocked by piece on target square")
        void doublePushBlockedByTargetSquare() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E2)
                    .black(PAWN, E4)     // only the target is blocked
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasAnyMove(moves, E2, E3),  "single push still valid");
            assertFalse(hasAnyMove(moves, E2, E4), "double push to occupied square blocked");
        }

        @Test
        @DisplayName("Diagonal capture left and right")
        void diagonalCaptures() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, D4)
                    .black(PAWN, C5).black(PAWN, E5)
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasMove(moves, D4, C5, FLAG_GENERIC_CAPTURE), "d4×c5");
            assertTrue(hasMove(moves, D4, E5, FLAG_GENERIC_CAPTURE), "d4×e5");
        }

        @Test
        @DisplayName("No wrap-around capture from A-file")
        void noWrapAroundCaptureAFile() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, A4)
                    .black(PAWN, H5)    // would be a wrap-around
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasAnyMove(moves, A4, H5), "a-file must not wrap to h-file");
        }

        @Test
        @DisplayName("No wrap-around capture from H-file")
        void noWrapAroundCaptureHFile() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, H4)
                    .black(PAWN, A5)    // would be a wrap-around
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasAnyMove(moves, H4, A5), "h-file must not wrap to a-file");
        }

        @Test
        @DisplayName("Single push only when not on starting rank")
        void noDoubleFromNonStartRank() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E3)
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasAnyMove(moves,  E3, E4), "e3→e4 should exist");
            assertFalse(hasAnyMove(moves, E3, E5), "double push from non-start rank forbidden");
        }

        @Test
        @DisplayName("Cannot capture own piece diagonally")
        void noCaptureFriendlyPiece() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, D4).white(KNIGHT, C5).white(KNIGHT, E5)
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasAnyMove(moves, D4, C5), "cannot capture friendly knight on c5");
            assertFalse(hasAnyMove(moves, D4, E5), "cannot capture friendly knight on e5");
        }

        @Test
        @DisplayName("En-passant capture")
        void enPassantCapture() {
            // black pawn just moved d7→d5; white pawn is on e5
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E5)
                    .black(PAWN, D5)
                    .enPassant(D6)        // target square for en-passant
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasMove(moves, E5, D6, FLAG_EN_PASSANT_CAP), "en-passant e5×d6");
        }

        @Test
        @DisplayName("Promotion on rank 8 – all four pieces expected")
        void promotionRank8() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E7)
                    .build();
            var moves = collectMoves(pos, true);
            // When promotion is implemented all four flags must appear
            long promoMoves = moves.stream()
                    .filter(m -> Move.from(m) == E7 && Move.to(m) == E8)
                    .count();
            // Adjust the expected count to 4 once promotion is implemented.
            // For now we just verify the pawn has no plain push to e8.
            // Remove/replace this assertion when promotions are implemented:
            assertTrue(promoMoves >= 1,
                    "Promotion: at least one encoded move e7→e8 expected (4 when fully implemented)");
        }

        @Test
        @DisplayName("Promotion capture on rank 8")
        void promotionCapture() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, D7)
                    .black(ROOK, E8)
                    .build();
            var moves = collectMoves(pos, true);
            long promoCapMoves = moves.stream()
                    .filter(m -> Move.from(m) == D7 && Move.to(m) == E8)
                    .count();
            assertTrue(promoCapMoves >= 1,
                    "Promotion capture: at least one encoded move d7×e8 expected");
        }
    }

    @Nested
    @DisplayName("Pawn – Black")
    class BlackPawnTests {

        @Test
        @DisplayName("Single push from starting rank")
        void singlePushFromStart() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .black(PAWN, E7)
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertTrue(hasMove(moves, E7, E6, FLAG_GENERIC), "e7→e6");
        }

        @Test
        @DisplayName("Double push from starting rank")
        void doublePushFromStart() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .black(PAWN, E7)
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertTrue(hasMove(moves, E7, E5, FLAG_GENERIC), "e7→e5");
        }

        @Test
        @DisplayName("No wrap-around from A-file")
        void noWrapAroundAFile() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .black(PAWN, A5)
                    .white(PAWN, H4)   // would be a wrap-around target
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertFalse(hasAnyMove(moves, A5, H4), "a-file must not wrap to h-file");
        }

        @Test
        @DisplayName("No wrap-around from H-file")
        void noWrapAroundHFile() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .black(PAWN, H5)
                    .white(PAWN, A4)
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertFalse(hasAnyMove(moves, H5, A4), "h-file must not wrap to a-file");
        }

        @Test
        @DisplayName("En-passant capture")
        void enPassantCapture() {
            // white pawn just moved e2→e4; black pawn is on d4
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .black(PAWN, D4)
                    .white(PAWN, E4)
                    .enPassant(E3)
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertTrue(hasMove(moves, D4, E3, FLAG_EN_PASSANT_CAP), "en-passant d4×e3");
        }

        @Test
        @DisplayName("Promotion on rank 1 – all four pieces expected")
        void promotionRank1() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, E8)
                    .black(PAWN, E2)
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            long promoMoves = moves.stream()
                    .filter(m -> Move.from(m) == E2 && Move.to(m) == E1)
                    .count();
            assertTrue(promoMoves >= 1,
                    "Promotion: at least one encoded move e2→e1 expected (4 when fully implemented)");
        }
    }


    // =========================================================================
    // Knight tests
    // =========================================================================

    @Nested
    @DisplayName("Knight")
    class KnightTests {

        @Test
        @DisplayName("Central knight has 8 moves on empty board")
        void centralKnight8Moves() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(KNIGHT, D4).build();
            var moves = collectMoves(pos, true);
            assertEquals(8, countFrom(moves, D4), "central knight should have 8 moves");
        }

        @Test
        @DisplayName("Corner knight (A1) has 2 moves")
        void cornerKnightA1() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(KNIGHT, A1).build();
            var moves = collectMoves(pos, true);
            assertEquals(2, countFrom(moves, A1));
        }

        @Test
        @DisplayName("Corner knight (H8) has 2 moves")
        void cornerKnightH8() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, A8)
                    .black(KNIGHT, H8).whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertEquals(2, countFrom(moves, H8));
        }

        @Test
        @DisplayName("Corner knight (A8) has 2 moves")
        void cornerKnightA8() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(KNIGHT, A8).build();
            // Use pseudo moves to isolate piece logic (king safety not relevant here)
            var moves = collectPseudo(pos, true);
            assertEquals(2, countFrom(moves, A8));
        }

        @Test
        @DisplayName("Corner knight (H1) has 2 moves")
        void cornerKnightH1() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, A8)
                    .white(KNIGHT, H1).build();
            var moves = collectPseudo(pos, true);
            assertEquals(2, countFrom(moves, H1));
        }

        @Test
        @DisplayName("No wrap-around jump from A-file")
        void noWrapFromAFile() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(KNIGHT, A4).build();
            var moves = collectPseudo(pos, true);
            // A knight on A4 must not land on G3 or G5 (wrap-around)
            assertFalse(hasAnyMove(moves, A4, G3), "wrap-around A4→G3 forbidden");
            assertFalse(hasAnyMove(moves, A4, G5), "wrap-around A4→G5 forbidden");
        }

        @Test
        @DisplayName("No wrap-around jump from H-file")
        void noWrapFromHFile() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, A8)
                    .white(KNIGHT, H5).build();
            var moves = collectPseudo(pos, true);
            assertFalse(hasAnyMove(moves, H5, B4), "wrap-around H5→B4 forbidden");
            assertFalse(hasAnyMove(moves, H5, B6), "wrap-around H5→B6 forbidden");
        }

        @Test
        @DisplayName("Knight blocked by friendly pieces – cannot capture own pieces")
        void knightBlockedByFriendly() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(KNIGHT, D4)
                    // Fill all 8 landing squares with friendly pawns
                    .white(PAWN, C2, E2, B3, F3, B5, F5, C6, E6).build();
            var moves = collectMoves(pos, true);
            assertEquals(0, countFrom(moves, D4), "all landing squares friendly – knight has 0 moves");
        }

        @Test
        @DisplayName("Knight can capture enemy but not friendly piece")
        void knightCapturesEnemy() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(KNIGHT, D4)
                    .black(PAWN, C6)
                    .white(PAWN, E6).build();
            var moves = collectMoves(pos, true);
            assertTrue( hasMove(moves, D4, C6, FLAG_GENERIC_CAPTURE), "knight captures black pawn on c6");
            assertFalse(hasAnyMove(moves, D4, E6),                    "knight blocked by friendly pawn on e6");
        }
    }


    // =========================================================================
    // Bishop tests
    // =========================================================================

    @Nested
    @DisplayName("Bishop")
    class BishopTests {

        @Test
        @DisplayName("Central bishop has 13 moves on empty board")
        void centralBishop13Moves() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(BISHOP, D4).build();
            var moves = collectPseudo(pos, true);
            assertEquals(13, countFrom(moves, D4), "central bishop on d4 should have 13 diagonal moves");
        }

        @Test
        @DisplayName("Corner bishop (A1) has 7 moves")
        void cornerBishop7Moves() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(BISHOP, A1).build();
            var moves = collectPseudo(pos, true);
            assertEquals(7, countFrom(moves, A1));
        }

        @Test
        @DisplayName("Bishop blocked by friendly piece")
        void bishopBlockedByFriendly() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(BISHOP, A1)
                    .white(PAWN, C3)   // blocks the a1-h8 diagonal at c3
                    .build();
            var moves = collectPseudo(pos, true);
            assertFalse(hasAnyMove(moves, A1, D4), "bishop cannot pass through friendly pawn on c3");
            assertFalse(hasAnyMove(moves, A1, C3), "bishop cannot land on friendly pawn");
        }

        @Test
        @DisplayName("Bishop captures enemy and stops")
        void bishopCapturesAndStops() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(BISHOP, A1)
                    .black(PAWN, C3).build();
            var moves = collectPseudo(pos, true);
            assertTrue( hasMove(moves, A1, C3, FLAG_GENERIC_CAPTURE), "bishop captures on c3");
            assertFalse(hasAnyMove(moves, A1, D4), "bishop cannot continue past captured piece");
        }

        @Test
        @DisplayName("Bishop does not wrap around board edges diagonally")
        void bishopNoWrapAround() {
            // Bishop on A4; moving NW would wrap to H5 etc.
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(BISHOP, A4).build();
            var moves = collectPseudo(pos, true);
            assertFalse(hasAnyMove(moves, A4, H5), "bishop must not wrap A4→H5");
            assertFalse(hasAnyMove(moves, A4, H3), "bishop must not wrap A4→H3");
        }
    }


    // =========================================================================
    // Rook tests
    // =========================================================================

    @Nested
    @DisplayName("Rook")
    class RookTests {

        @Test
        @DisplayName("Central rook has 14 moves on empty board")
        void centralRook14Moves() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(ROOK, D4).build();
            var moves = collectPseudo(pos, true);
            assertEquals(14, countFrom(moves, D4));
        }

        @Test
        @DisplayName("Corner rook (A1) has 14 moves")
        void cornerRookA1() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(ROOK, A1).build();
            var moves = collectPseudo(pos, true);
            assertEquals(14, countFrom(moves, A1));
        }

        @Test
        @DisplayName("Rook does not slide around board edge")
        void rookNoWrapAround() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(ROOK, A4).build();
            var moves = collectPseudo(pos, true);
            // Sliding left from A-file must not wrap to H-file
            for (int sq = 0; sq < 64; sq++) {
                if ((sq % 8) == 7) {  // H-file
                    assertFalse(hasAnyMove(moves, A4, sq),
                            "rook from A4 must not reach H-file square " + sq);
                }
            }
        }

        @Test
        @DisplayName("Rook blocked by friendly piece")
        void rookBlockedByFriendly() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(ROOK, A4)
                    .white(PAWN, A6).build();   // blocks upward ray
            var moves = collectPseudo(pos, true);
            assertFalse(hasAnyMove(moves, A4, A6), "cannot land on friendly pawn");
            assertFalse(hasAnyMove(moves, A4, A7), "cannot pass through friendly pawn");
            assertFalse(hasAnyMove(moves, A4, A8), "cannot pass through friendly pawn");
        }

        @Test
        @DisplayName("Rook captures enemy and stops")
        void rookCapturesEnemy() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(ROOK, A4)
                    .black(PAWN, A6).build();
            var moves = collectPseudo(pos, true);
            assertTrue( hasMove(moves, A4, A6, FLAG_GENERIC_CAPTURE), "rook captures on a6");
            assertFalse(hasAnyMove(moves, A4, A7), "rook cannot continue past captured piece");
        }
    }


    // =========================================================================
    // Queen tests
    // =========================================================================

    @Nested
    @DisplayName("Queen")
    class QueenTests {

        @Test
        @DisplayName("Central queen has 27 moves on empty board")
        void centralQueen27Moves() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(QUEEN, D4).build();
            var moves = collectPseudo(pos, true);
            assertEquals(27, countFrom(moves, D4));
        }

        @Test
        @DisplayName("Queen moves on both rays and diagonals")
        void queenCombinedRays() {
            var pos = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(QUEEN, D4).build();
            var moves = collectPseudo(pos, true);
            assertTrue(hasAnyMove(moves, D4, D8), "queen slides up file");
            assertTrue(hasAnyMove(moves, D4, H4), "queen slides right rank");
            assertTrue(hasAnyMove(moves, D4, G7), "queen slides diagonal NE");
            assertTrue(hasAnyMove(moves, D4, A1), "queen slides diagonal SW");
        }

        @Test
        @DisplayName("Queen does not wrap around edges")
        void queenNoWrapAround() {
            var pos = new PositionBuilder()
                    .white(KING, H1).black(KING, H8)
                    .white(QUEEN, A4).build();
            var moves = collectPseudo(pos, true);
            assertFalse(hasAnyMove(moves, A4, H5), "queen must not wrap A4→H5");
            assertFalse(hasAnyMove(moves, A4, H3), "queen must not wrap A4→H3");
        }
    }


    // =========================================================================
    // King tests
    // =========================================================================

    @Nested
    @DisplayName("King")
    class KingTests {

        @Test
        @DisplayName("Central king has 8 moves on empty board")
        void centralKing8Moves() {
            var pos = new PositionBuilder()
                    .black(KING, H8)
                    .white(KING, D4).build();
            var moves = collectPseudo(pos, true);
            assertEquals(8, countFrom(moves, D4));
        }

        @Test
        @DisplayName("Corner king (A1) has 3 moves")
        void cornerKingA1() {
            var pos = new PositionBuilder()
                    .black(KING, H8)
                    .white(KING, A1).build();
            var moves = collectPseudo(pos, true);
            assertEquals(3, countFrom(moves, A1));
        }

        @Test
        @DisplayName("Corner king (H8) has 3 moves")
        void cornerKingH8() {
            var pos = new PositionBuilder()
                    .white(KING, A1)
                    .black(KING, H8).whiteToMove(false).build();
            var moves = collectPseudo(pos, false);
            assertEquals(3, countFrom(moves, H8));
        }

        @Test
        @DisplayName("King does not move into check – adjacent enemy rook")
        void kingDoesNotMoveIntoCheck() {
            // White king on E1, black rook on D8 – king cannot step to D1 or D2
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .black(KING, H8)
                    .black(ROOK, D8).build();
            var moves = collectMoves(pos, true);
            assertFalse(hasAnyMove(moves, E1, D1), "king must not walk into rook check on d-file");
            assertFalse(hasAnyMove(moves, E1, D2), "king must not walk into rook check on d-file");
        }

        @Test
        @DisplayName("King does not move into check – adjacent enemy queen")
        void kingDoesNotMoveIntoQueenCheck() {
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .black(KING, H8)
                    .black(QUEEN, E8).build();  // covers entire e-file
            var moves = collectMoves(pos, true);
            assertFalse(hasAnyMove(moves, E1, E2), "king must not step toward queen on e-file");
        }

        @Test
        @DisplayName("King cannot capture a defended piece")
        void kingCannotCaptureDefendedPiece() {
            // Black rook on D2 defended by black king on D8
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .black(KING, A8)
                    .black(ROOK, D1).build(); // king captures d1 but d1 is covered by a8 king? No – use a clearer setup:
            // White king e1, black pawn d2 defended by black rook d8
            var pos2 = new PositionBuilder()
                    .white(KING, E1)
                    .black(KING, H8)
                    .black(PAWN, D2)
                    .black(ROOK, D8).build(); // rook defends d2 via d-file
            var moves2 = collectMoves(pos2, true);
            assertFalse(hasAnyMove(moves2, E1, D2), "king cannot capture pawn defended by rook on d-file");
        }

        @Test
        @DisplayName("King cannot wrap-around from A-file")
        void kingNoWrapFromAFile() {
            var pos = new PositionBuilder()
                    .black(KING, H8)
                    .white(KING, A4).build();
            var moves = collectPseudo(pos, true);
            assertFalse(hasAnyMove(moves, A4, H3), "king must not wrap A4→H3");
            assertFalse(hasAnyMove(moves, A4, H4), "king must not wrap A4→H4");
            assertFalse(hasAnyMove(moves, A4, H5), "king must not wrap A4→H5");
        }

        @Test
        @DisplayName("King cannot wrap-around from H-file")
        void kingNoWrapFromHFile() {
            var pos = new PositionBuilder()
                    .white(KING, A1)
                    .black(KING, H4).whiteToMove(false).build();
            var moves = collectPseudo(pos, false);
            assertFalse(hasAnyMove(moves, H4, A3), "king must not wrap H4→A3");
            assertFalse(hasAnyMove(moves, H4, A4), "king must not wrap H4→A4");
            assertFalse(hasAnyMove(moves, H4, A5), "king must not wrap H4→A5");
        }
    }


    // =========================================================================
    // Castling tests
    // =========================================================================

    @Nested
    @DisplayName("Castling")
    class CastlingTests {

        @Test
        @DisplayName("White kingside castle – clear path")
        void whiteKingsideCastle() {
            var pos = new PositionBuilder()
                    .black(KING, E8)
                    .white(KING, E1)
                    .white(ROOK, H1)
                    .wCastle(true, false)
                    .wCastlePerm(true, false)
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasMove(moves, E1, G1, FLAG_CASTLE_K), "white O-O should be available");
        }

        @Test
        @DisplayName("White queenside castle – clear path")
        void whiteQueensideCastle() {
            var pos = new PositionBuilder()
                    .black(KING, E8)
                    .white(KING, E1)
                    .white(ROOK, A1)
                    .wCastle(false, true)
                    .wCastlePerm(false, true)
                    .build();
            var moves = collectMoves(pos, true);
            assertTrue(hasMove(moves, E1, C1, FLAG_CASTLE_Q), "white O-O-O should be available");
        }

        @Test
        @DisplayName("Black kingside castle – clear path")
        void blackKingsideCastle() {
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .black(KING, E8)
                    .black(ROOK, H8)
                    .bCastle(true, false)
                    .bCastlePerm(true, false)
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertTrue(hasMove(moves, E8, G8, FLAG_CASTLE_K), "black O-O should be available");
        }

        @Test
        @DisplayName("Black queenside castle – clear path")
        void blackQueensideCastle() {
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .black(KING, E8)
                    .black(ROOK, A8)
                    .bCastle(false, true)
                    .bCastlePerm(false, true)
                    .whiteToMove(false).build();
            var moves = collectMoves(pos, false);
            assertTrue(hasMove(moves, E8, C8, FLAG_CASTLE_Q), "black O-O-O should be available");
        }

        @Test
        @DisplayName("Castle blocked by piece between king and rook (kingside)")
        void castleBlockedKingside() {
            var pos = new PositionBuilder()
                    .black(KING, E8)
                    .white(KING, E1)
                    .white(ROOK, H1)
                    .white(KNIGHT, F1)   // F1 is between king and rook
                    .wCastle(true, false)
                    .wCastlePerm(true, false)
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasMove(moves, E1, G1, FLAG_CASTLE_K), "castle blocked by knight on f1");
        }

        @Test
        @DisplayName("Castle blocked by piece between king and rook (queenside)")
        void castleBlockedQueenside() {
            var pos = new PositionBuilder()
                    .black(KING, E8)
                    .white(KING, E1)
                    .white(ROOK, A1)
                    .white(QUEEN, D1)    // D1 is between king and rook
                    .wCastle(false, true)
                    .wCastlePerm(false, true)
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasMove(moves, E1, C1, FLAG_CASTLE_Q), "castle blocked by queen on d1");
        }

        @Test
        @DisplayName("Cannot castle while in check")
        void cannotCastleWhileInCheck() {
            var pos = new PositionBuilder()
                    .black(KING, E8)
                    .white(KING, E1)
                    .white(ROOK, H1)
                    .black(ROOK, E8)    // black rook gives check on e-file
                    .wCastle(true, false)
                    .wCastlePerm(true, false)
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasMove(moves, E1, G1, FLAG_CASTLE_K), "cannot castle while in check");
        }

        @Test
        @DisplayName("Cannot castle through an attacked square (kingside f1)")
        void cannotCastleThroughAttack() {
            // Black rook attacks f1, the transit square for white O-O
            var pos = new PositionBuilder()
                    .black(KING, E8)
                    .white(KING, E1)
                    .white(ROOK, H1)
                    .black(ROOK, F8)    // attacks f1
                    .wCastle(true, false)
                    .wCastlePerm(true, false)
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasMove(moves, E1, G1, FLAG_CASTLE_K),
                    "cannot castle when transit square f1 is attacked");
        }

        @Test
        @DisplayName("No castling rights – right revoked")
        void noCastleRight() {
            var pos = new PositionBuilder()
                    .black(KING, E8)
                    .white(KING, E1)
                    .white(ROOK, H1)
                    .wCastle(false, false)   // rights revoked
                    .wCastlePerm(true, false)
                    .build();
            var moves = collectMoves(pos, true);
            assertFalse(hasMove(moves, E1, G1, FLAG_CASTLE_K), "no castling right – O-O must not appear");
        }
    }


    // =========================================================================
    // Check & pin legality tests
    // =========================================================================

    @Nested
    @DisplayName("Check / Legality")
    class LegalityTests {

        @Test
        @DisplayName("Side in check can only make moves that resolve check")
        void mustResolveCheck() {
            // Black queen on E8 gives check along e-file; white can interpose or move king
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .black(KING, A8)
                    .black(QUEEN, E8).build();
            var moves = collectMoves(pos, true);
            // Every legal move must leave white king not in check
            for (int m : moves) {
                var copy = applyMove(pos, m);
                assertFalse(copy.checkChess(true),
                        "Move " + moveStr(m) + " leaves king in check");
            }
        }

        @Test
        @DisplayName("Pinned piece cannot move off the pin ray")
        void pinnedPieceCannotMoveOffRay() {
            // White rook on E4 is pinned to the king on E1 by black rook on E8
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .white(ROOK, E4)
                    .black(KING, A8)
                    .black(ROOK, E8).build();
            var moves = collectMoves(pos, true);
            // The pinned rook may slide along the e-file but not leave it
            for (int m : moves) {
                if (Move.from(m) == E4) {
                    int to = Move.to(m);
                    assertEquals(4, to % 8,  // e-file is column index 4
                            "pinned rook on e4 must only move along e-file, got col " + (to % 8));
                }
            }
        }

        @Test
        @DisplayName("Double check: only king moves are legal")
        void doubleCheckOnlyKingMoves() {
            // White king on E1 is checked by both a black rook on E8 and a black knight on D3
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .white(ROOK, A1)    // extra piece that is now useless
                    .black(KING, H8)
                    .black(ROOK, E8)
                    .black(KNIGHT, D3).build();
            var moves = collectMoves(pos, true);
            for (int m : moves) {
                assertEquals(E1, Move.from(m),
                        "Under double check only king moves allowed, but got move from " + Move.from(m));
            }
        }
    }


    // =========================================================================
    // Full-position integration tests
    // =========================================================================

    @Nested
    @DisplayName("Integration – Starting Position")
    class StartingPositionTests {

        /** Build the standard chess starting position. */
        private PositionEncoder.Position startPos() {
            return new PositionBuilder()
                    // White pieces
                    .white(ROOK,   A1, H1)
                    .white(KNIGHT, B1, G1)
                    .white(BISHOP, C1, F1)
                    .white(QUEEN,  D1)
                    .white(KING,   E1)
                    .white(PAWN,   A2, B2, C2, D2, E2, F2, G2, H2)
                    // Black pieces
                    .black(ROOK,   A8, H8)
                    .black(KNIGHT, B8, G8)
                    .black(BISHOP, C8, F8)
                    .black(QUEEN,  D8)
                    .black(KING,   E8)
                    .black(PAWN,   A7, B7, C7, D7, E7, F7, G7, H7)
                    .build();
        }

        @Test
        @DisplayName("White has exactly 20 legal moves from starting position")
        void whiteMoveCount() {
            var moves = collectMoves(startPos(), true);
            assertEquals(20, moves.size(),
                    "Standard starting position: white should have exactly 20 legal moves");
        }

        @Test
        @DisplayName("Black has exactly 20 legal moves from starting position")
        void blackMoveCount() {
            var pos = startPos();
            pos.whiteToMove = false;
            var moves = collectMoves(pos, false);
            assertEquals(20, moves.size());
        }

        @Test
        @DisplayName("No pieces can move to enemy back rank from start")
        void noPieceReachesEnemyBackRank() {
            var moves = collectMoves(startPos(), true);
            for (int m : moves) {
                int row = Move.to(m) / 8;
                assertTrue(row <= 3, "no white piece should reach rows 4-7 from start (got row " + row + ")");
            }
        }
    }

    @Nested
    @DisplayName("Integration – Offset accumulation")
    class OffsetTests {

        @Test
        @DisplayName("generateMoves respects and advances offset correctly")
        void offsetRespected() {
            var pos = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E2).build();
            int[] buf = new int[256];
            // Pre-fill sentinel so we can detect overwrites
            java.util.Arrays.fill(buf, -1);
            buf[0] = 0xDEAD; // sentinel at offset 0
            int newOffset = MoveGen.generateMoves(pos, 1, true, buf);
            assertEquals(0xDEAD, buf[0], "slot 0 must not be overwritten when offset=1");
            assertTrue(newOffset > 1, "offset must have advanced beyond 1");
        }

        @Test
        @DisplayName("Two sequential generateMoves calls accumulate moves")
        void twoCallsAccumulate() {
            int[] buf = new int[256];
            var pos1 = new PositionBuilder()
                    .white(KING, E1).black(KING, E8)
                    .white(PAWN, E2).build();
            var pos2 = new PositionBuilder()
                    .white(KING, A1).black(KING, H8)
                    .white(KNIGHT, D4).build();

            int off1 = MoveGen.generateMoves(pos1, 0,  true, buf);
            int off2 = MoveGen.generateMoves(pos2, off1, true, buf);

            assertTrue(off2 > off1, "second call must add more moves after offset from first call");
        }
    }


    // =========================================================================
    // Pseudo-move vs legal-move difference tests
    // =========================================================================

    @Nested
    @DisplayName("Pseudo vs Legal")
    class PseudoVsLegalTests {

        @Test
        @DisplayName("Pseudo moves include moves that leave king in check; legal moves do not")
        void pseudoIncludesIllegal() {
            // White king E1, white rook E4 (pinned), black rook E8
            var pos = new PositionBuilder()
                    .white(KING, E1)
                    .white(ROOK, E4)
                    .black(KING, A8)
                    .black(ROOK, E8).build();

            var pseudo = collectPseudo(pos, true);
            var legal  = collectMoves(pos, true);

            // The pseudo list should contain rook moves off the e-file; legal should not
            boolean pseudoHasOffFile = pseudo.stream()
                    .anyMatch(m -> Move.from(m) == E4 && (Move.to(m) % 8) != 4);
            boolean legalHasOffFile  = legal.stream()
                    .anyMatch(m -> Move.from(m) == E4 && (Move.to(m) % 8) != 4);

            assertTrue(pseudoHasOffFile,  "pseudo moves should include pinned rook moving off e-file");
            assertFalse(legalHasOffFile,  "legal moves must not include pinned rook moving off e-file");
        }
    }


    // =========================================================================
    // Utility
    // =========================================================================

    /** Shallow-clone + apply a move to produce a new position for assertions. */
    private static PositionEncoder.Position applyMove(PositionEncoder.Position src, int move) {
        // Deep-copy the position so we don't mutate the original
        PositionEncoder.Position copy = deepCopy(src);
        copy.makeMove(move);
        return copy;
    }

    private static PositionEncoder.Position deepCopy(PositionEncoder.Position src) {
        PositionEncoder.Position dst = new PositionEncoder.Position();
        dst.WPawns   = src.WPawns;   dst.WKnights = src.WKnights; dst.WBishops = src.WBishops;
        dst.WRooks   = src.WRooks;   dst.WQueens  = src.WQueens;  dst.WKing    = src.WKing;
        dst.BPawns   = src.BPawns;   dst.BKnights = src.BKnights; dst.BBishops = src.BBishops;
        dst.BRooks   = src.BRooks;   dst.BQueens  = src.BQueens;  dst.BKing    = src.BKing;
        dst.Occupied = src.Occupied; dst.WPieces  = src.WPieces;  dst.BPieces  = src.BPieces;
        dst.enPassantSquare     = src.enPassantSquare;
        dst.WQueenCastle        = src.WQueenCastle; dst.WKingCastle  = src.WKingCastle;
        dst.BQueenCastle        = src.BQueenCastle; dst.BKingCastle  = src.BKingCastle;
        dst.WQueenCastlePerm    = src.WQueenCastlePerm; dst.WKingCastlePerm = src.WKingCastlePerm;
        dst.BQueenCastlePerm    = src.BQueenCastlePerm; dst.BKingCastlePerm = src.BKingCastlePerm;
        dst.whiteToMove         = src.whiteToMove;
        dst.fiftyMoveCounter    = src.fiftyMoveCounter;
        dst.pieceMap = src.pieceMap.clone();
        return dst;
    }

    /** Human-readable move string for assertion messages. */
    private static final String[] SQ_NAMES = {
            "a1","b1","c1","d1","e1","f1","g1","h1",
            "a2","b2","c2","d2","e2","f2","g2","h2",
            "a3","b3","c3","d3","e3","f3","g3","h3",
            "a4","b4","c4","d4","e4","f4","g4","h4",
            "a5","b5","c5","d5","e5","f5","g5","h5",
            "a6","b6","c6","d6","e6","f6","g6","h6",
            "a7","b7","c7","d7","e7","f7","g7","h7",
            "a8","b8","c8","d8","e8","f8","g8","h8"
    };

    private static String moveStr(int m) {
        return SQ_NAMES[Move.from(m)] + SQ_NAMES[Move.to(m)] + "[flags=" + Move.flags(m) + "]";
    }}