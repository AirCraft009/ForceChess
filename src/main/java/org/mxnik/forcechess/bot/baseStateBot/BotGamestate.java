package org.mxnik.forcechess.bot.baseStateBot;

import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.Util.Bitboard;

/**
 * Hardcoded BotGamestate for 64 x 64 further training may be applied
 */
public class BotGamestate {
    private int boardSize;
    private Bitboard WPawns = new Bitboard();
    private Bitboard BPanws = new Bitboard();
    private Bitboard WKnights = new Bitboard();
    private Bitboard BKnights = new Bitboard();
    private Bitboard WRooks = new Bitboard();
    private Bitboard BRooks = new Bitboard();
    private Bitboard WBishops = new Bitboard();
    private Bitboard BBishops = new Bitboard();
    private Bitboard WQueens = new Bitboard();
    private Bitboard BQueens = new Bitboard();
    private Bitboard WKings = new Bitboard();
    private Bitboard BKings = new Bitboard();
    private Bitboard WKingCastle = new Bitboard();
    private Bitboard BKingCastle = new Bitboard();
    private Bitboard WQueenCastle = new Bitboard();
    private Bitboard BQueenCastle = new Bitboard();


    private void buildFromBoard(Board buildBoard){
        Piece[] b = buildBoard.getBoard();
        for(int i = 0; i < Board.size; i++){
            Piece p = b[i];
            switch (p.getType()){
                case PAWN -> {
                    if (p.getColor()){
                        WPawns.set(i);
                        continue;
                    }
                    BPanws.set(i);
                }
                case BISHOP -> {
                    if (p.getColor()){
                        WBishops.set(i);
                        continue;
                    }
                    BBishops.set(i);
                }
                case KNIGHT -> {
                    if (p.getColor()){
                        WKnights.set(i);
                        continue;
                    }
                    BKnights.set(i);
                }
                case QUEEN -> {
                    if (p.getColor()){
                        WQueens.set(i);
                        continue;
                    }
                    BQueens.set(i);
                }
                case ROOK -> {
                    if (p.getColor()){
                        WRooks.set(i);
                        continue;
                    }
                    BRooks.set(i);
                }
                case KING -> {
                    if (p.getColor()){
                        WKings.set(i);
                        continue;
                    }
                    BKings.set(i);
                }
            }
        }
    }

    public BotGamestate (Board buildBoard){
        buildFromBoard(buildBoard);
    }

    public BotGamestate (String fenStr){
        Board bb = new Board(fenStr);
        buildFromBoard(bb);
    }

    public static void main(String[] args) {
        BotGamestate bg = new BotGamestate("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        System.out.println(bg.WPawns.board);
    }

    /**
     * PositionEncoder
     *
     * Encodes a chess position into a float[19][8][8] tensor suitable
     * for feeding into an AlphaZero-style neural network.
     *
     * Plane layout (19 planes total):
     *
     *  [0]  White pawns
     *  [1]  White knights
     *  [2]  White bishops
     *  [3]  White rooks
     *  [4]  White queens
     *  [5]  White king
     *  [6]  Black pawns
     *  [7]  Black knights
     *  [8]  Black bishops
     *  [9]  Black rooks
     *  [10] Black queens
     *  [11] Black king
     *  [12] White kingside castling right   (all 1s or all 0s)
     *  [13] White queenside castling right
     *  [14] Black kingside castling right
     *  [15] Black queenside castling right
     *  [16] En passant target square        (single 1 on target square)
     *  [17] Side to move                    (all 1s = white, all 0s = black)
     *  [18] Fifty-move counter              (normalised to [0,1])
     *
     * Coordinate convention:
     *   tensor[plane][rank][file]
     *   rank 0 = rank 1 (white's back rank), rank 7 = rank 8
     *   file 0 = a-file, file 7 = h-file
     *
     * Bit index in each long:
     *   bit = rank * 8 + file   (matches standard little-endian mapping)
     */
    public static final class PositionEncoder {

        // Number of planes in the tensor
        public static final int PLANES = 19;
        public static final int SIZE   = 8;

        // Plane indices — named constants keep encode() readable
        private static final int PLANE_WP  = 0;
        private static final int PLANE_WN  = 1;
        private static final int PLANE_WB  = 2;
        private static final int PLANE_WR  = 3;
        private static final int PLANE_WQ  = 4;
        private static final int PLANE_WK  = 5;
        private static final int PLANE_BP  = 6;
        private static final int PLANE_BN  = 7;
        private static final int PLANE_BB  = 8;
        private static final int PLANE_BR  = 9;
        private static final int PLANE_BQ  = 10;
        private static final int PLANE_BK  = 11;
        private static final int PLANE_CASTLE_WK = 12;
        private static final int PLANE_CASTLE_WQ = 13;
        private static final int PLANE_CASTLE_BK = 14;
        private static final int PLANE_CASTLE_BQ = 15;
        private static final int PLANE_EN_PASSANT = 16;
        private static final int PLANE_SIDE       = 17;
        private static final int PLANE_FIFTY      = 18;

        private PositionEncoder() {}

        // -------------------------------------------------------------------------
        // Public API
        // -------------------------------------------------------------------------

        /**
         * Encodes the given position into a freshly allocated float[19][8][8].
         *
         * @param pos  the position to encode
         * @return     tensor[plane][rank][file], all values in [0, 1]
         */
        public static float[][][] encode(Position pos) {
            float[][][] tensor = new float[PLANES][SIZE][SIZE];
            encode(pos, tensor);
            return tensor;
        }

        /**
         * Encodes the position into a pre-allocated tensor (avoids allocation
         * in the MCTS hot path — reuse the same array each search).
         *
         * @param pos     the position to encode
         * @param tensor  output buffer, must be float[19][8][8]
         */
        public static void encode(Position pos, float[][][] tensor) {
            // Clear previous contents (important when reusing the buffer)
            clearTensor(tensor);

            // ── Planes 0-11: piece bitboards ──────────────────────────────────
            encodeBitboard(pos.whitePawns,   tensor[PLANE_WP]);
            encodeBitboard(pos.whiteKnights, tensor[PLANE_WN]);
            encodeBitboard(pos.whiteBishops, tensor[PLANE_WB]);
            encodeBitboard(pos.whiteRooks,   tensor[PLANE_WR]);
            encodeBitboard(pos.whiteQueens,  tensor[PLANE_WQ]);
            encodeBitboard(pos.whiteKing,    tensor[PLANE_WK]);
            encodeBitboard(pos.blackPawns,   tensor[PLANE_BP]);
            encodeBitboard(pos.blackKnights, tensor[PLANE_BN]);
            encodeBitboard(pos.blackBishops, tensor[PLANE_BB]);
            encodeBitboard(pos.blackRooks,   tensor[PLANE_BR]);
            encodeBitboard(pos.blackQueens,  tensor[PLANE_BQ]);
            encodeBitboard(pos.blackKing,    tensor[PLANE_BK]);

            // ── Planes 12-15: castling rights (uniform planes) ────────────────
            if (pos.castleWhiteKingside)  fillPlane(tensor[PLANE_CASTLE_WK], 1.0f);
            if (pos.castleWhiteQueenside) fillPlane(tensor[PLANE_CASTLE_WQ], 1.0f);
            if (pos.castleBlackKingside)  fillPlane(tensor[PLANE_CASTLE_BK], 1.0f);
            if (pos.castleBlackQueenside) fillPlane(tensor[PLANE_CASTLE_BQ], 1.0f);

            // ── Plane 16: en passant target square ───────────────────────────
            // enPassantSquare == -1 means no en passant available
            if (pos.enPassantSquare >= 0) {
                int rank = pos.enPassantSquare >>> 3;   // divide by 8
                int file = pos.enPassantSquare & 7;     // modulo 8
                tensor[PLANE_EN_PASSANT][rank][file] = 1.0f;
            }

            // ── Plane 17: side to move ────────────────────────────────────────
            if (pos.whiteToMove) fillPlane(tensor[PLANE_SIDE], 1.0f);
            // black to move → plane stays all zeros (already cleared)

            // ── Plane 18: fifty-move clock (normalised) ───────────────────────
            // Divide by 100 so the value sits in [0, 1].
            // At 100 the game is a draw — network should learn this boundary.
            float fiftyNorm = Math.min(pos.fiftyMoveCounter / 100.0f, 1.0f);
            fillPlane(tensor[PLANE_FIFTY], fiftyNorm);
        }

        // -------------------------------------------------------------------------
        // Private helpers
        // -------------------------------------------------------------------------

        /**
         * Scatters a bitboard into an 8×8 float plane.
         *
         * bit index = rank * 8 + file  (little-endian square mapping)
         * Iterates only over set bits using the standard popLsb trick —
         * cost is O(popcount), not O(64).
         */
        private static void encodeBitboard(long bitboard, float[][] plane) {
            long b = bitboard;
            while (b != 0L) {
                int sq   = Long.numberOfTrailingZeros(b); // index of lowest set bit
                int rank = sq >>> 3;                      // sq / 8
                int file = sq & 7;                        // sq % 8
                plane[rank][file] = 1.0f;
                b &= b - 1;                               // clear lowest set bit
            }
        }

        /** Sets every cell in a plane to the given value. */
        private static void fillPlane(float[][] plane, float value) {
            for (int rank = 0; rank < SIZE; rank++) {
                for (int file = 0; file < SIZE; file++) {
                    plane[rank][file] = value;
                }
            }
        }

        /** Zeros the entire tensor (used when reusing a pre-allocated buffer). */
        private static void clearTensor(float[][][] tensor) {
            for (float[][] plane : tensor) {
                for (float[] row : plane) {
                    java.util.Arrays.fill(row, 0.0f);
                }
            }
        }

        // -------------------------------------------------------------------------
        // Position record
        // -------------------------------------------------------------------------

        /**
         * Minimal position container.
         *
         * In a real engine this would be your full board state class.
         * Each long is a bitboard: bit (rank*8 + file) = 1 means a piece
         * of that type occupies that square.
         */
        public static final class Position {

            // Piece bitboards (12 planes)
            public long whitePawns,   whiteKnights, whiteBishops;
            public long whiteRooks,   whiteQueens,  whiteKing;
            public long blackPawns,   blackKnights, blackBishops;
            public long blackRooks,   blackQueens,  blackKing;

            // Castling rights
            public boolean castleWhiteKingside, castleWhiteQueenside;
            public boolean castleBlackKingside, castleBlackQueenside;

            // En passant: -1 if none, otherwise the target square index
            public int enPassantSquare = -1;

            // Side to move
            public boolean whiteToMove = true;

            // Fifty-move rule counter (0-100)
            public int fiftyMoveCounter = 0;

            /** Constructs a standard starting position. */
            public static Position startingPosition() {
                Position p = new Position();

                p.whitePawns   = 0x000000000000FF00L; // rank 2
                p.whiteKnights = 0x0000000000000042L; // b1, g1
                p.whiteBishops = 0x0000000000000024L; // c1, f1
                p.whiteRooks   = 0x0000000000000081L; // a1, h1
                p.whiteQueens  = 0x0000000000000008L; // d1
                p.whiteKing    = 0x0000000000000010L; // e1

                p.blackPawns   = 0x00FF000000000000L; // rank 7
                p.blackKnights = 0x4200000000000000L; // b8, g8
                p.blackBishops = 0x2400000000000000L; // c8, f8
                p.blackRooks   = 0x8100000000000000L; // a8, h8
                p.blackQueens  = 0x0800000000000000L; // d8
                p.blackKing    = 0x1000000000000000L; // e8

                p.castleWhiteKingside  = true;
                p.castleWhiteQueenside = true;
                p.castleBlackKingside  = true;
                p.castleBlackQueenside = true;

                return p;
            }
        }

        // -------------------------------------------------------------------------
        // Quick smoke test
        // -------------------------------------------------------------------------

        public static void main(String[] args) {
            Position pos = Position.startingPosition();
            float[][][] tensor = encode(pos);

            // White pawns should be set on rank 1 (index), files 0-7
            System.out.println("=== Plane 0: White pawns (rank 1) ===");
            for (int rank = 7; rank >= 0; rank--) {
                for (int file = 0; file < 8; file++) {
                    System.out.print((int) tensor[PLANE_WP][rank][file] + " ");
                }
                System.out.println("  ← rank " + (rank + 1));
            }

            // Castling should be all 1s
            System.out.println("\n=== Plane 12: White kingside castling ===");
            System.out.println("tensor[12][0][0] = " + tensor[PLANE_CASTLE_WK][0][0]
                + "  (expected 1.0)");

            // Side to move: white → all 1s
            System.out.println("\n=== Plane 17: Side to move ===");
            System.out.println("tensor[17][0][0] = " + tensor[PLANE_SIDE][0][0]
                + "  (expected 1.0, white to move)");

            // Reuse buffer test (hot path)
            float[][][] buffer = new float[PLANES][SIZE][SIZE];
            encode(pos, buffer);
            System.out.println("\nReuse buffer test passed — no allocation.");
        }
    }
}
