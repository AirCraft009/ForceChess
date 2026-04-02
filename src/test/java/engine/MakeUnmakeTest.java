package engine;

import org.junit.jupiter.api.*;
import org.mxnik.forcechess.Util.Bitboard;
import org.mxnik.forcechess.engine.bot.baseStateBot.Move;
import org.mxnik.forcechess.engine.bot.baseStateBot.Piece;
import org.mxnik.forcechess.engine.bot.baseStateBot.PositionEncoder;
import static org.mxnik.forcechess.engine.bot.baseStateBot.ChessSquares.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Make and unmake positions
 */
@DisplayName("Make / Unmake")
class MakeUnmakeTest {


    // Helpers

    static PositionEncoder.Position emptyPosition() {
        var pos = new PositionEncoder.Position();
        Arrays.fill(pos.pieceMap, (byte) Piece.EMPTY_PIECE);
        return pos;
    }

    static void place(PositionEncoder.Position pos, int color, int type, int sq) {
        pos.pieceMap[sq] = (byte) Piece.of(color, type);
        long bit = Bitboard.set(0L, sq);
        if (color == Piece.WHITE) {
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

    /**
     * Record to hold state of position so after unmake and the original can be compared
     */
    record Snapshot(
            long WPawns, long WKnights, long WBishops, long WRooks, long WQueens, long WKing,
            long BPawns, long BKnights, long BBishops, long BRooks, long BQueens, long BKing,
            long Occupied, long WPieces, long BPieces,
            boolean WQueenCastle, boolean WKingCastle,
            boolean BQueenCastle, boolean BKingCastle,
            int enPassantSquare,
            byte[] pieceMap
    ) {
        static Snapshot of(PositionEncoder.Position pos) {
            return new Snapshot(
                    pos.WPawns, pos.WKnights, pos.WBishops, pos.WRooks, pos.WQueens, pos.WKing,
                    pos.BPawns, pos.BKnights, pos.BBishops, pos.BRooks, pos.BQueens, pos.BKing,
                    pos.Occupied, pos.WPieces, pos.BPieces,
                    pos.WQueenCastle, pos.WKingCastle,
                    pos.BQueenCastle, pos.BKingCastle,
                    pos.enPassantSquare,
                    Arrays.copyOf(pos.pieceMap, 64)
            );
        }

        void assertRestoredIn(PositionEncoder.Position pos) {
            assertEquals(WPawns,   pos.WPawns,   "WPawns");
            assertEquals(WKnights, pos.WKnights, "WKnights");
            assertEquals(WBishops, pos.WBishops, "WBishops");
            assertEquals(WRooks,   pos.WRooks,   "WRooks");
            assertEquals(WQueens,  pos.WQueens,  "WQueens");
            assertEquals(WKing,    pos.WKing,    "WKing");
            assertEquals(BPawns,   pos.BPawns,   "BPawns");
            assertEquals(BKnights, pos.BKnights, "BKnights");
            assertEquals(BBishops, pos.BBishops, "BBishops");
            assertEquals(BRooks,   pos.BRooks,   "BRooks");
            assertEquals(BQueens,  pos.BQueens,  "BQueens");
            assertEquals(BKing,    pos.BKing,    "BKing");
            assertEquals(Occupied, pos.Occupied, "Occupied");
            assertEquals(WPieces,  pos.WPieces,  "WPieces");
            assertEquals(BPieces,  pos.BPieces,  "BPieces");
            assertEquals(WQueenCastle, pos.WQueenCastle, "WQueenCastle");
            assertEquals(WKingCastle,  pos.WKingCastle,  "WKingCastle");
            assertEquals(BQueenCastle, pos.BQueenCastle, "BQueenCastle");
            assertEquals(BKingCastle,  pos.BKingCastle,  "BKingCastle");
            assertEquals(enPassantSquare, pos.enPassantSquare, "enPassantSquare");
            assertArrayEquals(pieceMap, pos.pieceMap, "pieceMap");
        }
    }



    @Nested
    @DisplayName("Generic Flag")
    class QuietMoveTests {

        @Test
        @DisplayName("Piece appears on destination, vacates source")
        void pieceMovesToDestination() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.ROOK, A1);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, E8);

            int move = Move.of(A1, A5, Move.FLAG_GENERIC);
            pos.makeMove(move);

            assertEquals(Piece.of(Piece.WHITE, Piece.ROOK), pos.pieceMap[A5] & 0xFF, "Rook on A5");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[A1] & 0xFF,                 "A1 vacated");
            assertTrue(Bitboard.get(pos.WRooks, A5),  "WRooks bit set on A5");
            assertFalse(Bitboard.get(pos.WRooks, A1), "WRooks bit cleared on A1");
        }

        @Test
        @DisplayName("Full restore after unmake")
        void fullRestoreAfterUnmake() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.ROOK, A1);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, E8);

            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(A1, A5, Move.FLAG_GENERIC));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }

    // 2. Generic capture (FLAG_GENERIC_CAPTURE)

    @Nested
    @DisplayName("Generic capture")
    class GenericCaptureTests {

        @Test
        @DisplayName("Captured piece removed, attacker on destination")
        void capturedPieceRemoved() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.QUEEN, D1);
            place(pos, Piece.BLACK, Piece.PAWN,  D5);
            place(pos, Piece.WHITE, Piece.KING,  E1);
            place(pos, Piece.BLACK, Piece.KING,  E8);

            pos.makeMove(Move.of(D1, D5, Move.FLAG_GENERIC_CAPTURE));

            assertEquals(Piece.of(Piece.WHITE, Piece.QUEEN), pos.pieceMap[D5] & 0xFF, "Queen on D5");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[D1] & 0xFF,                  "D1 vacated");
            assertFalse(Bitboard.get(pos.BPawns, D5), "BPawns bit cleared on D5");
        }

        @Test
        @DisplayName("Full restore after unmake")
        void fullRestoreAfterUnmake() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.QUEEN, D1);
            place(pos, Piece.BLACK, Piece.PAWN,  D5);
            place(pos, Piece.WHITE, Piece.KING,  E1);
            place(pos, Piece.BLACK, Piece.KING,  E8);

            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(D1, D5, Move.FLAG_GENERIC_CAPTURE));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }

    // 3. Kingside castle (FLAG_CASTLE_K)

    @Nested
    @DisplayName("Kingside castle")
    class KingSideCastleTests {

        PositionEncoder.Position ready() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.WHITE, Piece.ROOK, H1);
            place(pos, Piece.BLACK, Piece.KING, E8);
            pos.WKingCastle     = true;
            return pos;
        }

        @Test
        @DisplayName("King lands on G1, rook lands on F1")
        void piecesOnCorrectSquares() {
            var pos = ready();
            pos.makeMove(Move.of(E1, G1, Move.FLAG_CASTLE_K));

            assertEquals(Piece.of(Piece.WHITE, Piece.KING), pos.pieceMap[G1] & 0xFF, "King on G1");
            assertEquals(Piece.of(Piece.WHITE, Piece.ROOK), pos.pieceMap[F1] & 0xFF, "Rook on F1");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[E1] & 0xFF, "E1 vacated");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[H1] & 0xFF, "H1 vacated");
        }

        @Test
        @DisplayName("Full restore after unmake")
        void fullRestoreAfterUnmake() {
            var pos = ready();
            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(E1, G1, Move.FLAG_CASTLE_K));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }

    // 4. Queenside castle (FLAG_CASTLE_Q)

    @Nested
    @DisplayName("Queenside castle")
    class QueenSideCastleTests {

        PositionEncoder.Position ready() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.WHITE, Piece.ROOK, A1);
            place(pos, Piece.BLACK, Piece.KING, E8);
            pos.WQueenCastle     = true;
            return pos;
        }

        @Test
        @DisplayName("King lands on C1, rook lands on D1")
        void piecesOnCorrectSquares() {
            var pos = ready();
            pos.makeMove(Move.of(E1, C1, Move.FLAG_CASTLE_Q));

            assertEquals(Piece.of(Piece.WHITE, Piece.KING), pos.pieceMap[C1] & 0xFF, "King on C1");
            assertEquals(Piece.of(Piece.WHITE, Piece.ROOK), pos.pieceMap[D1] & 0xFF, "Rook on D1");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[E1] & 0xFF, "E1 vacated");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[A1] & 0xFF, "A1 vacated");
        }

        @Test
        @DisplayName("Full restore after unmake")
        void fullRestoreAfterUnmake() {
            var pos = ready();
            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(E1, C1, Move.FLAG_CASTLE_Q));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }

    // 5. En passant capture (FLAG_EN_PASSANT_CAPTURE)

    @Nested
    @DisplayName("En passant capture")
    class EnPassantCaptureTests {

        @Test
        @DisplayName("Capturing pawn on D6, captured pawn on D5 removed")
        void capturedPawnRemoved() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E5);
            place(pos, Piece.BLACK, Piece.PAWN, D5); // just double-pushed
            pos.enPassantSquare = D6;
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, E8);

            pos.makeMove(Move.of(E5, D6, Move.FLAG_EN_PASSANT_CAPTURE));

            assertEquals(Piece.of(Piece.WHITE, Piece.PAWN), pos.pieceMap[D6] & 0xFF, "White pawn on D6");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[E5] & 0xFF, "E5 vacated");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[D5] & 0xFF, "D5 captured pawn removed");
            assertFalse(Bitboard.get(pos.BPawns, D5), "BPawns bit cleared on D5");
        }

        @Test
        @DisplayName("Full restore after unmake")
        void fullRestoreAfterUnmake() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E5);
            place(pos, Piece.BLACK, Piece.PAWN, D5);
            pos.enPassantSquare = D6;
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, E8);

            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(E5, D6, Move.FLAG_EN_PASSANT_CAPTURE));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }

    // 6. Promotions (quiet)

    @Nested
    @DisplayName("Promotion (quiet)")
    class PromotionTests {

        @Test
        @DisplayName("FLAG_PROMOTE_Q: pawn replaced by queen on rank 8")
        void promoteToQueen() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            pos.makeMove(Move.of(E7, E8, Move.FLAG_PROMOTE_Q));

            assertEquals(Piece.of(Piece.WHITE, Piece.QUEEN), pos.pieceMap[E8] & 0xFF, "Queen on E8");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[E7] & 0xFF, "E7 vacated");
            assertFalse(Bitboard.get(pos.WPawns,  E8), "No pawn bit on E8");
            assertTrue(Bitboard.get(pos.WQueens, E8),  "Queen bit set on E8");
        }

        @Test
        @DisplayName("FLAG_PROMOTE_R: pawn replaced by rook on rank 8")
        void promoteToRook() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            pos.makeMove(Move.of(E7, E8, Move.FLAG_PROMOTE_R));

            assertEquals(Piece.of(Piece.WHITE, Piece.ROOK), pos.pieceMap[E8] & 0xFF, "Rook on E8");
            assertTrue(Bitboard.get(pos.WRooks, E8), "Rook bit set on E8");
        }

        @Test
        @DisplayName("FLAG_PROMOTE_B: pawn replaced by bishop on rank 8")
        void promoteToBishop() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            pos.makeMove(Move.of(E7, E8, Move.FLAG_PROMOTE_B));

            assertEquals(Piece.of(Piece.WHITE, Piece.BISHOP), pos.pieceMap[E8] & 0xFF, "Bishop on E8");
            assertTrue(Bitboard.get(pos.WBishops, E8), "Bishop bit set on E8");
        }

        @Test
        @DisplayName("FLAG_PROMOTE_N: pawn replaced by knight on rank 8")
        void promoteToKnight() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            pos.makeMove(Move.of(E7, E8, Move.FLAG_PROMOTE_N));

            assertEquals(Piece.of(Piece.WHITE, Piece.KNIGHT), pos.pieceMap[E8] & 0xFF, "Knight on E8");
            assertTrue(Bitboard.get(pos.WKnights, E8), "Knight bit set on E8");
        }

        @Test
        @DisplayName("Full restore after unmake (queen promotion)")
        void fullRestoreAfterUnmake() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(E7, E8, Move.FLAG_PROMOTE_Q));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }

    // 7. Promotion captures

    @Nested
    @DisplayName("Promotion capture")
    class PromotionCaptureTests {

        @Test
        @DisplayName("FLAG_PROMOTE_Q_CAPTURE: pawn captures and promotes to queen")
        void promoteQCapture() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.BLACK, Piece.ROOK, F8); // capturable
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            pos.makeMove(Move.of(E7, F8, Move.FLAG_PROMOTE_Q_CAPTURE));

            assertEquals(Piece.of(Piece.WHITE, Piece.QUEEN), pos.pieceMap[F8] & 0xFF, "Queen on F8");
            assertEquals(Piece.EMPTY_PIECE, pos.pieceMap[E7] & 0xFF, "E7 vacated");
            assertFalse(Bitboard.get(pos.BRooks, F8), "BRooks bit cleared on F8");
            assertTrue(Bitboard.get(pos.WQueens, F8),  "WQueens bit set on F8");
        }

        @Test
        @DisplayName("Full restore after unmake (queen promotion capture)")
        void fullRestoreAfterUnmake() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.BLACK, Piece.ROOK, F8);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(E7, F8, Move.FLAG_PROMOTE_Q_CAPTURE));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }

        @Test
        @DisplayName("Full restore after unmake (knight promotion capture)")
        void fullRestoreKnightCapture() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.PAWN, E7);
            place(pos, Piece.BLACK, Piece.ROOK, F8);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, A8);

            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(E7, F8, Move.FLAG_PROMOTE_N_CAPTURE));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }

    // 8. Make / unmake sequence (multiple moves)

    @Nested
    @DisplayName("Multi-move sequence")
    class MultiMoveTests {

        @Test
        @DisplayName("Two quiet moves then two unmakes restores original position")
        void twoMovesAndUnmake() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.ROOK,   A1);
            place(pos, Piece.WHITE, Piece.KNIGHT, B1);
            place(pos, Piece.WHITE, Piece.KING,   E1);
            place(pos, Piece.BLACK, Piece.KING,   E8);

            var before = Snapshot.of(pos);

            int undo1 = pos.makeMove(Move.of(A1, A4, Move.FLAG_GENERIC));
            int undo2 = pos.makeMove(Move.of(B1, C3, Move.FLAG_GENERIC));

            pos.unmakeMove(undo2);
            pos.unmakeMove(undo1);

            before.assertRestoredIn(pos);
        }

        @Test
        @DisplayName("Capture then unmake restores captured piece")
        void captureThenUnmake() {
            var pos = emptyPosition();
            place(pos, Piece.WHITE, Piece.ROOK, A1);
            place(pos, Piece.BLACK, Piece.PAWN, A6);
            place(pos, Piece.WHITE, Piece.KING, E1);
            place(pos, Piece.BLACK, Piece.KING, E8);

            var before = Snapshot.of(pos);
            int undo = pos.makeMove(Move.of(A1, A6, Move.FLAG_GENERIC_CAPTURE));
            pos.unmakeMove(undo);
            before.assertRestoredIn(pos);
        }
    }
}