package org.mxnik.forcechess.bot.baseStateBot;

import org.mxnik.forcechess.Util.Bitboard;

import java.util.Arrays;

/**
 * PositionEncoder
 *
 * Encodes a chess position into a float[19][8][8] tensor suitable
 * for feeding into an AlphaZero-style NN.
 *
 * Plane layout (21 planes total):
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
 *  [16] White double pawn moves
 *  [17] Black double pawn moves
 *  [18] En passant target square        (single 1 on target square)
 *  [19] Side to move                    (all 1s = white, all 0s = black)
 *  [20] Fifty-move counter              (normalised to [0,1])
 *
 * Coordinate convention:
 *   tensor[plane][rank][file]
 *   rank 0 = rank 1 (white's back rank), rank 7 = rank 8
 *   file 0 = a-file, file 7 = h-file
 *
 * Bit index in each long:
 *   bit = rank * 8 + file
 */
public final class PositionEncoder {

    // Number of planes in the tensor
    public static final int PLANES = 21;
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
    private static final int PLANE_DOUBLE_PW = 16;
    private static final int PLANE_DOUBLE_PB = 17;
    private static final int PLANE_EN_PASSANT = 18;
    private static final int PLANE_SIDE       = 19;
    private static final int PLANE_FIFTY      = 20;

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
     * Encodes the position into a pre-allocated tensor
     * (avoids allocation in the MCTS hot path — reuse the same array each search).
     *
     * @param pos     the position to encode
     * @param tensor  output buffer, must be float[19][8][8]
     */
    public static void encode(Position pos, float[][][] tensor) {
        // Clear previous contents (important when reusing the buffer)
        clearTensor(tensor);

        // 0 - 11 piece layers
        encodeBitboard(pos.WPawns.board,   tensor[PLANE_WP]);
        encodeBitboard(pos.WKnights.board, tensor[PLANE_WN]);
        encodeBitboard(pos.WBishops.board, tensor[PLANE_WB]);
        encodeBitboard(pos.WRooks.board,   tensor[PLANE_WR]);
        encodeBitboard(pos.WQueens.board,  tensor[PLANE_WQ]);
        encodeBitboard(pos.WKing.board,    tensor[PLANE_WK]);

        encodeBitboard(pos.BPawns.board,   tensor[PLANE_BP]);
        encodeBitboard(pos.BKnights.board, tensor[PLANE_BN]);
        encodeBitboard(pos.BBishops.board, tensor[PLANE_BB]);
        encodeBitboard(pos.BRooks.board,   tensor[PLANE_BR]);
        encodeBitboard(pos.BQueens.board,  tensor[PLANE_BQ]);
        encodeBitboard(pos.BKing.board,    tensor[PLANE_BK]);




        // 12 - 15 volle Planes 1 / 0
        if (pos.WKingCastle) fillPlane(tensor[PLANE_CASTLE_WK], 1.0f);
        if (pos.WQueenCastle) fillPlane(tensor[PLANE_CASTLE_WQ], 1.0f);
        if (pos.BKingCastle) fillPlane(tensor[PLANE_CASTLE_BK], 1.0f);
        if (pos.BQueenCastle) fillPlane(tensor[PLANE_CASTLE_BQ], 1.0f);

        // 16 - 17 double pawn moves
        encodeBitboard(pos.WDoublePawnMove.board, tensor[PLANE_DOUBLE_PW]);
        encodeBitboard(pos.BDoublePawnMove.board, tensor[PLANE_DOUBLE_PB]);

        // 16 enPassant
        // while a bitboard alr exists opening setting a single bit in the file is easier
        // enPassantSquare == -1 means no en passant available
        if (pos.enPassantSquare >= 0) {
            int rank = pos.enPassantSquare >>> 3;   // divide by 8
            int file = pos.enPassantSquare & 7;     // modulo 8
            tensor[PLANE_EN_PASSANT][rank][file] = 1.0f;
        }

        // side to move
        if (pos.whiteToMove) fillPlane(tensor[PLANE_SIDE], 1.0f);
        // black to move → plane stays all zeros (already cleared)

        // 50 move Regel.
        float fiftyNorm = Math.min(pos.fiftyMoveCounter / 100.0f, 1.0f);
        fillPlane(tensor[PLANE_FIFTY], fiftyNorm);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Bitboard split on a plane of the tensor
     *
     *
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
     * Position Conatiner
     * Contains bitboards and entire gamestate
     *
     * usage - Movegen
     * usage - plane encoding
     */
    public static final class Position {

        public Bitboard WPawns;
        public Bitboard BPawns;
        public Bitboard WKnights;
        public Bitboard BKnights;
        public Bitboard WRooks;
        public Bitboard BRooks;
        public Bitboard WBishops;
        public Bitboard BBishops;
        public Bitboard WQueens;
        public Bitboard BQueens;
        public Bitboard WKing;
        public Bitboard BKing;
        public Bitboard WDoublePawnMove;
        public Bitboard BDoublePawnMove;
        public long enPassant;
        public long Occupied;
        public long WPieces;
        public long BPieces;


        public boolean WQueenCastle;
        public boolean WKingCastle;
        public boolean BQueenCastle;
        public boolean BKingCastle;

        // En passant: -1 if none, otherwise the target square index
        public int enPassantSquare = -1;

        // Side to move
        public boolean whiteToMove = true;

        // Fifty-move rule counter (0-100)
        public int fiftyMoveCounter = 0;

        /** Constructs a standard starting position. */
        public static Position startingPosition() {
            Position p = new Position();

            // White Piece Bitboards
            p.WPawns   = new Bitboard(0x000000000000FF00L); // rank 2
            p.WKnights = new Bitboard(0x0000000000000042L); // b1, g1
            p.WBishops = new Bitboard(0x0000000000000024L); // c1, f1
            p.WRooks   = new Bitboard(0x0000000000000081L); // a1, h1
            p.WQueens  = new Bitboard(0x0000000000000008L); // d1
            p.WKing    = new Bitboard(0x0000000000000010L); // e1

            // Black Piece Bitboards
            p.BPawns  = new Bitboard(0x00FF000000000000L); // rank 7
            p.BKnights = new Bitboard(0x4200000000000000L); // b8, g8
            p.BBishops = new Bitboard(0x2400000000000000L); // c8, f8
            p.BRooks   = new Bitboard(0x8100000000000000L); // a8, h8
            p.BQueens  = new Bitboard(0x0800000000000000L); // d8
            p.BKing    = new Bitboard(0x1000000000000000L); // e8


            // Extra layers (context)
            p.enPassant = 0; // none
            p.WDoublePawnMove = new Bitboard(0x000000000000FF00L); // rank 2
            p.BDoublePawnMove = new Bitboard(0x00FF000000000000L); // rank 7
            p.BQueenCastle = false;
            p.WQueenCastle = false;
            p.BKingCastle = false;
            p.WKingCastle = false;

            //Helper Layers
            p.Occupied = 0xFFFF00000000FFFFL; // rank 1-2 & 7-8
            p.WPieces = 0xFFFF000000000000L;
            p.BPieces = 0x000000000000FFFFL;
            return p;
        }

        public void updateHelper(){
            BPieces = BPawns.board | BBishops.board | BKnights.board | BRooks.board | BQueens.board | BKing.board;
            WPieces = WPawns.board | WBishops.board | WKnights.board | WRooks.board | WQueens.board | WKing.board;
            Occupied = BPieces | WPieces;
        }
    }

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
            + "  (expected 0.0)");

        // Side to move: white → all 1s
        System.out.println("\n=== Plane 17: Side to move ===");
        System.out.println("tensor[17][0][0] = " + tensor[PLANE_SIDE][0][0]
            + "  (expected 1.0, white to move)");

        // Reuse buffer test (hot path)
        float[][][] buffer = new float[PLANES][SIZE][SIZE];
        encode(pos, buffer);
        System.out.println("\nReuse buffer test passed — no allocation.");

        // Move gen test (Pre allocated moves - hot path)
        int[] moves = new int[256];
        System.out.println(MoveGen.generateMoves(pos, 0, true, moves));
        System.out.println(Move.from(moves[17]));
        System.out.println(Move.to(moves[17]));
    }
}
