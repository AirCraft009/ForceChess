package org.mxnik.forcechess.engine.bot.baseStateBot;

import org.mxnik.forcechess.Util.Bitboard;
import org.mxnik.forcechess.Util.Helper;

import java.util.Arrays;

import static org.mxnik.forcechess.Util.RayDetection.*;
import static org.mxnik.forcechess.Util.RayDetection.KNIGHT_COL;
import static org.mxnik.forcechess.engine.bot.baseStateBot.PositionBuilder.*;

/**
 * PositionEncoder
 *
 * Encodes a chess position into a float[21][8][8] tensor suitable
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

    public static final int PLANES = 21;
    public static final int SIZE   = 8;

    private static final int PLANE_WP         = 0;
    private static final int PLANE_WN         = 1;
    private static final int PLANE_WB         = 2;
    private static final int PLANE_WR         = 3;
    private static final int PLANE_WQ         = 4;
    private static final int PLANE_WK         = 5;
    private static final int PLANE_BP         = 6;
    private static final int PLANE_BN         = 7;
    private static final int PLANE_BB         = 8;
    private static final int PLANE_BR         = 9;
    private static final int PLANE_BQ         = 10;
    private static final int PLANE_BK         = 11;
    private static final int PLANE_CASTLE_WK  = 12;
    private static final int PLANE_CASTLE_WQ  = 13;
    private static final int PLANE_CASTLE_BK  = 14;
    private static final int PLANE_CASTLE_BQ  = 15;
    private static final int PLANE_DOUBLE_PW  = 16;
    private static final int PLANE_DOUBLE_PB  = 17;
    private static final int PLANE_EN_PASSANT = 18;
    private static final int PLANE_SIDE       = 19;
    private static final int PLANE_FIFTY      = 20;

    private PositionEncoder() {}

    // Public API

    public static float[][][] encode(Position pos) {
        float[][][] tensor = new float[PLANES][SIZE][SIZE];
        encode(pos, tensor);
        return tensor;
    }

    /**
     * Encodes into a pre-allocated tensor — reuse this buffer in the MCTS hot path.
     */
    public static void encode(Position pos, float[][][] tensor) {
        clearTensor(tensor);

        // Piece planes 0–11
        encodeBitboard(pos.WPawns,   tensor[PLANE_WP]);
        encodeBitboard(pos.WKnights, tensor[PLANE_WN]);
        encodeBitboard(pos.WBishops, tensor[PLANE_WB]);
        encodeBitboard(pos.WRooks,   tensor[PLANE_WR]);
        encodeBitboard(pos.WQueens,  tensor[PLANE_WQ]);
        encodeBitboard(pos.WKing,    tensor[PLANE_WK]);

        encodeBitboard(pos.BPawns,   tensor[PLANE_BP]);
        encodeBitboard(pos.BKnights, tensor[PLANE_BN]);
        encodeBitboard(pos.BBishops, tensor[PLANE_BB]);
        encodeBitboard(pos.BRooks,   tensor[PLANE_BR]);
        encodeBitboard(pos.BQueens,  tensor[PLANE_BQ]);
        encodeBitboard(pos.BKing,    tensor[PLANE_BK]);

        // Castling rights — uniform planes
        if (pos.WKingCastle)  fillPlane(tensor[PLANE_CASTLE_WK], 1.0f);
        if (pos.WQueenCastle) fillPlane(tensor[PLANE_CASTLE_WQ], 1.0f);
        if (pos.BKingCastle)  fillPlane(tensor[PLANE_CASTLE_BK], 1.0f);
        if (pos.BQueenCastle) fillPlane(tensor[PLANE_CASTLE_BQ], 1.0f);

        // Double pawn move planes
        encodeBitboard(pos.WDoublePawnMove, tensor[PLANE_DOUBLE_PW]);
        encodeBitboard(pos.BDoublePawnMove, tensor[PLANE_DOUBLE_PB]);

        // En passant — single square
        if (pos.enPassantSquare >= 0) {
            int rank = pos.enPassantSquare >>> 3;
            int file = pos.enPassantSquare & 7;
            tensor[PLANE_EN_PASSANT][rank][file] = 1.0f;
        }

        // Side to move
        if (pos.whiteToMove) fillPlane(tensor[PLANE_SIDE], 1.0f);

        // Fifty-move rule
        fillPlane(tensor[PLANE_FIFTY], Math.min(pos.fiftyMoveCounter / 100.0f, 1.0f));
    }

    // Private helpers

    private static void encodeBitboard(long bitboard, float[][] plane) {
        long b = bitboard;
        while (b != 0L) {
            int sq   = Bitboard.lsb(b);
            int rank = sq >>> 3;
            int file = sq & 7;
            plane[rank][file] = 1.0f;
            b = Bitboard.popLsb(b); // returns board with LSB cleared
        }
    }

    private static void fillPlane(float[][] plane, float value) {
        for (float[] row : plane) java.util.Arrays.fill(row, value);
    }

    private static void clearTensor(float[][][] tensor) {
        for (float[][] plane : tensor)
            for (float[] row : plane)
                java.util.Arrays.fill(row, 0.0f);
    }

    // Position

    /**
     * Position container — all bitboards are raw longs.
     * Use the static Bitboard helpers (Bitboard.set, Bitboard.clear, etc.)
     * to manipulate them; assign the returned value back to the field.
     *
     *   pos.WPawns = Bitboard.set(pos.WPawns, sq);
     *   pos.WPawns = Bitboard.clear(pos.WPawns, sq);
     */
    public static final class Position {

        //  Piece bitboards (raw longs)
        public long WPawns;
        public long WKnights;
        public long WBishops;
        public long WRooks;
        public long WQueens;
        public long WKing;

        public long BPawns;
        public long BKnights;
        public long BBishops;
        public long BRooks;
        public long BQueens;
        public long BKing;

        //  Context bitboards
        public long WDoublePawnMove;
        public long BDoublePawnMove;
        public long enPassant;      // bitboard form (kept for ray use)
        public long Occupied;
        public long WPieces;
        public long BPieces;

        //  Castling permissions (may castle if rights arise)
        public boolean WQueenCastlePerm;
        public boolean WKingCastlePerm;
        public boolean BQueenCastlePerm;
        public boolean BKingCastlePerm;

        //  Castling rights (current game state)
        public boolean WQueenCastle;
        public boolean WKingCastle;
        public boolean BQueenCastle;
        public boolean BKingCastle;

        //  En passant: -1 if none, otherwise target square index
        public int enPassantSquare = -1;

        // Side to move
        public boolean whiteToMove = true;

        //  Fifty-move rule counter (0–100)
        public int fiftyMoveCounter = 0;

        // Piece map: Color(1 bit) | PieceType(3 bits) per square
        public byte[] pieceMap = new byte[64];


        // Make / Unmake

        public int makeMove(int move){
            int undo = makeMoveCore(move);
            updateHelper();
            return undo;
        }

        private int makeMoveCore(int move) {
            int from      = Move.from(move);
            int to        = Move.to(move);
            int moveType  = Move.flags(move);

            switch (moveType) {
                case Move.FLAG_CASTLE_K_CAPTURE,
                     Move.FLAG_CASTLE_Q_CAPTURE -> throw new IllegalStateException(
                        "Castle can't be of type capture: " + Integer.toBinaryString(moveType));

                case Move.FLAG_EN_PASSANT -> throw new IllegalStateException(
                        "En-Passant can't be a normal (non-capture): " + Integer.toBinaryString(moveType));

                case Move.FLAG_CASTLE_K-> {

                    // rook in the corner -> one left of the kings new pos
                    movePiece(from + (distRight(from)), to - 1);
                    movePiece(from, to);

                    return UndoMoveInfo.of(move, Piece.EMPTY_PIECE);
                }
                case Move.FLAG_CASTLE_Q -> {
                    // rook in the corner -> one right of the kings new pos
                    movePiece(from - (distLeft(from)), to + 1);
                    movePiece(from, to);

                    return UndoMoveInfo.of(move, Piece.EMPTY_PIECE);
                }
                case Move.FLAG_GENERIC_CAPTURE -> {
                    int takenPiece = pieceMap[to];
                    movePiece(from, to);
                    return UndoMoveInfo.of(move, takenPiece);
                }
                case Move.FLAG_EN_PASSANT_CAPTURE -> {
                    // flip the lowest bit (color) because other piece is a pawn
                    int takenPiece = pieceMap[from] ^ 1;
                    movePiece(from, to);
                    return UndoMoveInfo.of(move, takenPiece);
                }
                default -> {
                    // FLAG_GENERIC and anything else
                    //TODO: handle Promotions
                    movePiece(from, to);
                    return UndoMoveInfo.of(move, Piece.EMPTY_PIECE);
                }
            }
        }

        public void unmakeMove(int undoInfo) {
            int move = UndoMoveInfo.move(undoInfo);
            int from      = Move.from(move);
            int to        = Move.to(move);
            int flags = Move.flags(move);

            boolean fAttack = Move.attackFromFlag(flags);
            int fType = Move.baseFlag(flags);

            movePiece(to, from);

            // first clears bit (does nothing no piece there) then sets it;
            MoveOnBoard(UndoMoveInfo.takenPiece(undoInfo), to, to);     // sets takenPiece -> if emptyPiece nothing is done;

            // only check baseType ignore attack bit
            switch (fType){
                case Move.FLAG_CASTLE_K -> {
                    // rook left of the king -> to the corner
                    movePiece(to - 1, from + (distRight(from)));
                }
                case Move.FLAG_CASTLE_Q -> {
                    // rook one right of the king -> to the corner
                    movePiece(to + 1, from - (distLeft(from)));
                }
                case Move.FLAG_EN_PASSANT -> {
                    // get the offset to the pawnField
                    int dir = Integer.compare(from, to) * 8;

                    MoveOnBoard(UndoMoveInfo.takenPiece(undoInfo),to + dir, to + dir);
                }
                default -> {
                    // TODO: Promotion
                }
            }

            updateHelper();
        }

        // Check detection

        public boolean checkChess(boolean color) {
            return color
                    ? checkChess(WKing, true)
                    : checkChess(BKing, false);
        }

        private boolean checkChess(long kingBoard, boolean kingColor) {
            int kingPos = Bitboard.lsb(kingBoard);
            int kingRow = Helper.getRow(kingPos);
            int kingCol = Helper.getCol(kingPos);

            // Rays (straight + diagonal)
            for (int dir = 0; dir < 8; dir++) {
                int dr = RAY_ROW[dir];
                int dc = RAY_COL[dir];
                int r = kingRow + dr;
                int c = kingCol + dc;
                boolean firstStep = true;

                while (r >= 0 && r < SIZE && c >= 0 && c < SIZE) {
                    int p = pieceMap[r * SIZE + c];

                    if (p != Piece.EMPTY_PIECE) {
                        if (Piece.color(p) != kingColor) {
                            int type = Piece.pieceT(p);
                            if (dir < 4) {
                                // straight rays Rook or Queen
                                if (type == Piece.ROOK || type == Piece.QUEEN) return true;
                            } else {
                                // diagonal rays Bishop or Queen
                                if (type == Piece.BISHOP || type == Piece.QUEEN) return true;

                                // Pawns only attack one square diagonal
                                if (firstStep && type == Piece.PAWN) {
                                    boolean pawnIsWhite = Piece.color(p);
                                    if (pawnIsWhite  && r < kingRow) return true;
                                    if (!pawnIsWhite && r > kingRow) return true;
                                }
                            }
                        }
                        break; // blocker — ray cut off
                    }

                    r += dr;
                    c += dc;
                    firstStep = false;
                }
            }

            // Knight jumps
            for (int k = 0; k < 8; k++) {
                int r = kingRow + KNIGHT_ROW[k];
                int c = kingCol + KNIGHT_COL[k];
                if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) continue;

                int p = pieceMap[r * SIZE + c];
                if (Piece.pieceT(p) != Piece.EMPTY_PIECE
                        && Piece.color(p) != kingColor
                        && Piece.pieceT(p) == Piece.KNIGHT) {
                    return true;
                }
            }

            // Adjacent enemy king
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int r = kingRow + dr;
                    int c = kingCol + dc;
                    if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) continue;

                    int p = pieceMap[r * SIZE + c];
                    if (Piece.pieceT(p) != Piece.EMPTY_PIECE
                            && Piece.color(p) != kingColor
                            && Piece.pieceT(p) == Piece.KING) {
                        return true;
                    }
                }
            }

            return false;
        }

        // Internal helpers

        private int distLeft(int pos){
            return pos % SIZE;
        }

        private int distRight(int pos){
            return SIZE - ((pos % SIZE) + 1);
        }

        private void movePiece(int from, int to) {
            int movedPiece = pieceMap[from];
            int takenPiece = pieceMap[to];

            // Move the piece on its own bitboard
            MoveOnBoard(movedPiece, from, to);

            // Remove any captured piece from its bitboard
            clearOnBoard(takenPiece, to);


            // Keep pieceMap in sync
            pieceMap[to]   = (byte) movedPiece;
            pieceMap[from] = (byte) Piece.EMPTY_PIECE;

            updateHelper();
        }

        /**
         * Moves a piece on its corresponding bitboard (clear from, set to).
         * Looks up the board by piece byte and reassigns the field.
         */
        private void MoveOnBoard(int piece, int from, int to) {
            boolean color = Piece.color(piece);
            int type      = Piece.pieceT(piece);

            if (color) { // white
                switch (type) {
                    case Piece.EMPTY_PIECE -> {}
                    case Piece.PAWN   -> { WPawns   = Bitboard.clear(WPawns,   from); WPawns   = Bitboard.set(WPawns,   to); }
                    case Piece.KNIGHT -> { WKnights = Bitboard.clear(WKnights, from); WKnights = Bitboard.set(WKnights, to); }
                    case Piece.BISHOP -> { WBishops = Bitboard.clear(WBishops, from); WBishops = Bitboard.set(WBishops, to); }
                    case Piece.ROOK   -> { WRooks   = Bitboard.clear(WRooks,   from); WRooks   = Bitboard.set(WRooks,   to); }
                    case Piece.QUEEN  -> { WQueens  = Bitboard.clear(WQueens,  from); WQueens  = Bitboard.set(WQueens,  to); }
                    case Piece.KING   -> { WKing    = Bitboard.clear(WKing,    from); WKing    = Bitboard.set(WKing,    to); }
                    default -> throw new InvalidPieceTypeException("MoveOnBoard: unknown white piece type " + type);
                }
            } else { // black
                switch (type) {
                    case Piece.EMPTY_PIECE -> {}
                    case Piece.PAWN   -> { BPawns   = Bitboard.clear(BPawns,   from); BPawns   = Bitboard.set(BPawns,   to); }
                    case Piece.KNIGHT -> { BKnights = Bitboard.clear(BKnights, from); BKnights = Bitboard.set(BKnights, to); }
                    case Piece.BISHOP -> { BBishops = Bitboard.clear(BBishops, from); BBishops = Bitboard.set(BBishops, to); }
                    case Piece.ROOK   -> { BRooks   = Bitboard.clear(BRooks,   from); BRooks   = Bitboard.set(BRooks,   to); }
                    case Piece.QUEEN  -> { BQueens  = Bitboard.clear(BQueens,  from); BQueens  = Bitboard.set(BQueens,  to); }
                    case Piece.KING   -> { BKing    = Bitboard.clear(BKing,    from); BKing    = Bitboard.set(BKing,    to); }
                    default -> throw new InvalidPieceTypeException("MoveOnBoard: unknown black piece type " + type);
                }
            }
        }

        /** Clears a square on the bitboard corresponding to the given piece byte. */
        private void clearOnBoard(int piece, int sq) {
            boolean color = Piece.color(piece);
            int type      = Piece.pieceT(piece);

            if (color) {
                switch (type) {
                    case Piece.EMPTY_PIECE -> {}
                    case Piece.PAWN   -> WPawns   = Bitboard.clear(WPawns,   sq);
                    case Piece.KNIGHT -> WKnights = Bitboard.clear(WKnights, sq);
                    case Piece.BISHOP -> WBishops = Bitboard.clear(WBishops, sq);
                    case Piece.ROOK   -> WRooks   = Bitboard.clear(WRooks,   sq);
                    case Piece.QUEEN  -> WQueens  = Bitboard.clear(WQueens,  sq);
                    case Piece.KING   -> WKing    = Bitboard.clear(WKing,    sq);
                    default -> throw new InvalidPieceTypeException("clearOnBoard: unknown white piece type " + type);
                }
            } else {
                switch (type) {
                    case Piece.EMPTY_PIECE -> {}
                    case Piece.PAWN   -> BPawns   = Bitboard.clear(BPawns,   sq);
                    case Piece.KNIGHT -> BKnights = Bitboard.clear(BKnights, sq);
                    case Piece.BISHOP -> BBishops = Bitboard.clear(BBishops, sq);
                    case Piece.ROOK   -> BRooks   = Bitboard.clear(BRooks,   sq);
                    case Piece.QUEEN  -> BQueens  = Bitboard.clear(BQueens,  sq);
                    case Piece.KING   -> BKing    = Bitboard.clear(BKing,    sq);
                    default -> throw new InvalidPieceTypeException("clearOnBoard: unknown black piece type " + type);
                }
            }
        }

        // Lookup helpers (return the long value, not a reference)

        /** Returns the current value of the bitboard for the given piece byte. */
        public long getLongFromPiece(int p) {
            return getLongFromPieceType(Piece.color(p), Piece.pieceT(p));
        }

        /** Returns the current value of the bitboard for a color + piece type. */
        public long getLongFromPieceType(boolean color, int type) {
            if (color) {
                return switch (type) {
                    case Piece.PAWN         -> WPawns;
                    case Piece.KNIGHT       -> WKnights;
                    case Piece.BISHOP       -> WBishops;
                    case Piece.ROOK         -> WRooks;
                    case Piece.QUEEN        -> WQueens;
                    case Piece.KING         -> WKing;
                    case Piece.EMPTY_PIECE  -> 0L;
                    default -> throw new InvalidPieceTypeException("No matching piece type: " + type);
                };
            } else {
                return switch (type) {
                    case Piece.PAWN         -> BPawns;
                    case Piece.KNIGHT       -> BKnights;
                    case Piece.BISHOP       -> BBishops;
                    case Piece.ROOK         -> BRooks;
                    case Piece.QUEEN        -> BQueens;
                    case Piece.KING         -> BKing;
                    case Piece.EMPTY_PIECE  -> 0L;
                    default -> throw new InvalidPieceTypeException("No matching piece type: " + type);
                };
            }
        }

        // default position

        public static Position StartingPosition() {
            Position p = new Position();

            p.WPawns   = 0x000000000000FF00L; // rank 2
            p.WKnights = 0x0000000000000042L; // b1, g1
            p.WBishops = 0x0000000000000024L; // c1, f1
            p.WRooks   = 0x0000000000000081L; // a1, h1
            p.WQueens  = 0x0000000000000008L; // d1
            p.WKing    = 0x0000000000000010L; // e1

            p.BPawns   = 0x00FF000000000000L; // rank 7
            p.BKnights = 0x4200000000000000L; // b8, g8
            p.BBishops = 0x2400000000000000L; // c8, f8
            p.BRooks   = 0x8100000000000000L; // a8, h8
            p.BQueens  = 0x0800000000000000L; // d8
            p.BKing    = 0x1000000000000000L; // e8

            p.WDoublePawnMove = 0x000000000000FF00L; // rank 2
            p.BDoublePawnMove = 0x00FF000000000000L; // rank 7
            p.enPassant       = 0L;

            p.Occupied = 0xFFFF00000000FFFFL;
            p.WPieces  = 0x000000000000FFFFL;
            p.BPieces  = 0xFFFF000000000000L;

            p.WKingCastle  = false;
            p.WQueenCastle = false;
            p.BKingCastle  = false;
            p.BQueenCastle = false;

            // White piece map
            p.pieceMap[0]  = Piece.WHITE | (Piece.ROOK   << 1);
            p.pieceMap[1]  = Piece.WHITE | (Piece.KNIGHT << 1);
            p.pieceMap[2]  = Piece.WHITE | (Piece.BISHOP << 1);
            p.pieceMap[3]  = Piece.WHITE | (Piece.QUEEN  << 1);
            p.pieceMap[4]  = Piece.WHITE | (Piece.KING   << 1);
            p.pieceMap[5]  = Piece.WHITE | (Piece.BISHOP << 1);
            p.pieceMap[6]  = Piece.WHITE | (Piece.KNIGHT << 1);
            p.pieceMap[7]  = Piece.WHITE | (Piece.ROOK   << 1);
            for (int i = 8; i < 16; i++)
                p.pieceMap[i] = Piece.WHITE | (Piece.PAWN << 1);

            // Black piece map
            p.pieceMap[56] = Piece.BLACK | (Piece.ROOK   << 1);
            p.pieceMap[57] = Piece.BLACK | (Piece.KNIGHT << 1);
            p.pieceMap[58] = Piece.BLACK | (Piece.BISHOP << 1);
            p.pieceMap[59] = Piece.BLACK | (Piece.KING   << 1); // NOTE: was QUEEN at 59 and KING at 60 — fixed to match bitboards (e8=60, d8=59)
            p.pieceMap[60] = Piece.BLACK | (Piece.QUEEN  << 1);
            p.pieceMap[61] = Piece.BLACK | (Piece.BISHOP << 1);
            p.pieceMap[62] = Piece.BLACK | (Piece.KNIGHT << 1);
            p.pieceMap[63] = Piece.BLACK | (Piece.ROOK   << 1);
            for (int i = 48; i < 56; i++)
                p.pieceMap[i] = Piece.BLACK | (Piece.PAWN << 1);

            return p;
        }

        public void updateHelper() {
            WPieces  = WPawns | WKnights | WBishops | WRooks | WQueens | WKing;
            BPieces  = BPawns | BKnights | BBishops | BRooks | BQueens | BKing;
            Occupied = WPieces | BPieces;
        }
    }

    public static void doublePushFromStart() {
        var pos = new PositionBuilder()
                .white(Piece.KING, E1).black(KING, E8)
                .white(PAWN, E2)
                .build();
        int[] moves = new int[100];
        int a = MoveGen.generateMoves(pos, 0, true, moves);
        for (int i = 0; i < a; i++) {
            System.out.printf("%d -> %d\n", Move.from(moves[i]), Move.to(moves[i]));
        }
        System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
    }


    public static void main(String[] args) {
        Position pos = Position.StartingPosition();
        float[][][] tensor = encode(pos);

        System.out.println("=== Plane 0: White pawns ===");
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++)
                System.out.print((int) tensor[PLANE_WP][rank][file] + " ");
            System.out.println("  ← rank " + (rank + 1));
        }

        System.out.println("\n=== Plane 12: White kingside castling (expect 0.0) ===");
        System.out.println("tensor[12][0][0] = " + tensor[PLANE_CASTLE_WK][0][0]);

        System.out.println("\n=== Plane 19: Side to move (expect 1.0 = white) ===");
        System.out.println("tensor[19][0][0] = " + tensor[PLANE_SIDE][0][0]);

        float[][][] buffer = new float[PLANES][SIZE][SIZE];
        encode(pos, buffer);
        System.out.println("\nReuse buffer test passed.");

        int[] moves = new int[256];
        int actLen = MoveGen.generatePseudoMoves(pos, 0, true, moves);


        for (int i = 0; i < actLen; i++) {
            System.out.printf(" from: %d -> to: %d%n",
                    Move.from(moves[i]), Move.to(moves[i]));
        }

        pos.makeMove(moves[5]);
        pos.makeMove(moves[14]);

        int off = MoveGen.generatePseudoMoves(pos, 0, false, moves);
        System.out.println("black");
        for (int i = 0; i < off; i++) {
            System.out.printf(" from: %d -> to: %d%n",
                    Move.from(moves[i]), Move.to(moves[i]));
        }
        pos.makeMove(moves[4]);
        System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
        System.out.println(Arrays.toString(pos.pieceMap));

        doublePushFromStart();
    }
}