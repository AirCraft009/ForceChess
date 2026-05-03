package org.mxnik.forcechess.Pos;

import org.mxnik.forcechess.Bitboard;
import org.mxnik.forcechess.GameState;

import java.util.Arrays;
import java.util.Objects;

import static org.mxnik.forcechess.RayDetection.*;
import static org.mxnik.forcechess.RayDetection.KNIGHT_COL;
import static org.mxnik.forcechess.ChessSquares.*;

/**
 * PositionEncoder
 * <p>
 * Encodes a chess position into a float[21][8][8] tensor suitable
 * for feeding into an AlphaZero-style NN.
 * <p>
 * Coordinate convention:
 *   tensor[plane][rank][file]
 *   rank 0 = rank 1 (white's back rank), rank 7 = rank 8
 *   file 0 = a-file, file 7 = h-file
 * <p>
 * Bit index in each long:
 *   bit = rank * 8 + file
 *   0 - 63
 *
 */
public final class PositionEncoder {

    public static final int PLANES = 21;
    public static final int SIZE   = 8;
    public static final int PLANE_SIZE = 64;
    public static final int TENSOR_SIZE = PLANES * SIZE * SIZE;

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

    public static float[] encodeFlat(Position pos) {
        float[] tensor = new float[TENSOR_SIZE];
        encode(pos, tensor);
        return tensor;
    }

    /**
     * Encodes into a pre-allocated tensor — reuse this buffer in the MCTS hot path.
     * - starting at the given offset
     */
    public static int encode(int offset, Position pos, float[] tensor) {
        // Only clear this slice, not the whole tensor
        Arrays.fill(tensor, offset, offset + TENSOR_SIZE - 1, 0);

        // Piece planes 0–11
        encodeBitboard(pos.WPawns,   offset + PLANE_WP * PLANE_SIZE, tensor);
        encodeBitboard(pos.WKnights, offset + PLANE_WN * PLANE_SIZE, tensor);
        encodeBitboard(pos.WBishops, offset + PLANE_WB * PLANE_SIZE, tensor);
        encodeBitboard(pos.WRooks,   offset + PLANE_WR * PLANE_SIZE, tensor);
        encodeBitboard(pos.WQueens,  offset + PLANE_WQ * PLANE_SIZE, tensor);
        encodeBitboard(pos.WKing,    offset + PLANE_WK * PLANE_SIZE, tensor);

        encodeBitboard(pos.BPawns,   offset + PLANE_BP * PLANE_SIZE, tensor);
        encodeBitboard(pos.BKnights, offset + PLANE_BN * PLANE_SIZE, tensor);
        encodeBitboard(pos.BBishops, offset + PLANE_BB * PLANE_SIZE, tensor);
        encodeBitboard(pos.BRooks,   offset + PLANE_BR * PLANE_SIZE, tensor);
        encodeBitboard(pos.BQueens,  offset + PLANE_BQ * PLANE_SIZE, tensor);
        encodeBitboard(pos.BKing,    offset + PLANE_BK * PLANE_SIZE, tensor);

        // Castling rights — uniform planes
        if (pos.WKingCastle)
            Arrays.fill(tensor,
                    offset + PLANE_CASTLE_WK * PLANE_SIZE,
                    offset + PLANE_CASTLE_WQ * PLANE_SIZE,
                    1.0f);

        if (pos.WQueenCastle)
            Arrays.fill(tensor,
                    offset + PLANE_CASTLE_WQ * PLANE_SIZE,
                    offset + PLANE_CASTLE_BK * PLANE_SIZE,
                    1.0f);

        if (pos.BKingCastle)
            Arrays.fill(tensor,
                    offset + PLANE_CASTLE_BK * PLANE_SIZE,
                    offset + PLANE_CASTLE_BQ * PLANE_SIZE,
                    1.0f);

        if (pos.BQueenCastle)
            Arrays.fill(tensor,
                    offset + PLANE_CASTLE_BQ * PLANE_SIZE,
                    offset + PLANE_DOUBLE_PW * PLANE_SIZE,
                    1.0f);

        // Double pawn move planes
        encodeBitboard(pos.WDoublePawnMove, offset + PLANE_DOUBLE_PW * PLANE_SIZE, tensor);
        encodeBitboard(pos.BDoublePawnMove, offset + PLANE_DOUBLE_PB * PLANE_SIZE, tensor);

        // En passant — single square
        if (pos.enPassantSquare >= 0) {
            tensor[offset + PLANE_EN_PASSANT * PLANE_SIZE + pos.enPassantSquare] = 1.0f;
        }

        // Side to move
        if (pos.whiteToMove)
            Arrays.fill(tensor,
                    offset + PLANE_SIDE * PLANE_SIZE,
                    offset + PLANE_FIFTY * PLANE_SIZE,
                    1.0f);

        // Fifty-move rule
        Arrays.fill(tensor,
                offset + PLANE_FIFTY * PLANE_SIZE,
                offset + TENSOR_SIZE-1,
                Math.min(pos.fiftyMoveCounter / 100.0f, 1.0f));

        return offset + TENSOR_SIZE;
    }

    /**
     * Encodes into a pre-allocated tensor — reuse this buffer in the MCTS hot path.
     */
    public static void encode(Position pos, float[] tensor) {
        encode(0, pos, tensor);
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

    private static void encodeBitboard(long bitboard, int offset, float[] plane) {
        long b = bitboard;
        while (b != 0L) {
            int sq   = Bitboard.lsb(b);
            int rank = sq >>> 3;
            int file = sq & 7;
            plane[offset + (rank * SIZE + file)] = 1.0f;
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
     * Use the static Bitboard helpers (Bitboard.set, Bitboard.clear, etc.)to manipulate them;
     * assign the returned value back to the field.
     * <p>
     *   example: <p>
     *   pos.WPawns = Bitboard.set(pos.WPawns, sq);
     *   pos.WPawns = Bitboard.clear(pos.WPawns, sq);
     * <p>
     * has a Public API for making moves, unmaking them, checking if the king is in danger.
     * I don't know if this is the most efficient Java code possible,
     * but it works and should be faster than any attempt using classes to model the state
     *
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
        public long Occupied;
        public long WPieces;
        public long BPieces;

        // pieceVals
        public static final int[] pieceVals = {0, 5, 3, 3, 0, 9, 1};        // king can't get taken so 0 since both sides always have one

        // Castling permissions (may castle if rights arise)
        // bit 0 = WKingC
        // bit 1 = WQueenC
        // bit 2 = BKingC
        // bit 3 = BQueenC
        public byte castlePerms;

        public static final byte W_KINGSIDE  = 0b0001;
        public static final byte W_QUEENSIDE = 0b0010;
        public static final byte B_KINGSIDE  = 0b0100;
        public static final byte B_QUEENSIDE = 0b1000;

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

        public int whiteMaterial;
        public int blackMaterial;


        // Make / Unmake

        public int makeMove(int move){
            int undo = makeMoveCore(move);
            whiteToMove = !whiteToMove;
            updateHelper();
            calcMat();
            fiftyMoveCounter ++;
            return undo;
        }


        private int makeMoveCore(int move) {
            int from      = Move.from(move);
            int to        = Move.to(move);
            int moveType  = Move.flags(move);

            //System.out.printf("making move %d -> %d\n", from, to);

            switch (moveType) {
                case Move.FLAG_CASTLE_K_CAPTURE,
                     Move.FLAG_CASTLE_Q_CAPTURE -> throw new IllegalStateException(
                        "Castle can't be of type capture: " + Integer.toBinaryString(moveType));

                case Move.FLAG_EN_PASSANT -> throw new IllegalStateException(
                        "En-Passant can't be a normal (non-capture): " + Integer.toBinaryString(moveType));

                case Move.FLAG_CASTLE_K-> {
                    // rook in the corner -> one left of the kings new pos
                    movePiece(from + (distRight(from)), to - 1);
                }
                case Move.FLAG_CASTLE_Q -> {
                    // rook in the corner -> one right of the kings new pos
                    movePiece(from - (distLeft(from)), to + 1);
                }
                case Move.FLAG_GENERIC_CAPTURE -> {}
                case Move.FLAG_EN_PASSANT_CAPTURE -> {
                    int dir = Integer.compare(from, to) * 8;
                    int p = pieceMap[to+dir];
                    clearOnBoard(p, to+dir);

                    byte currentPerms = castlePerms;
                    movePiece(from, to);
                    updateCastlePerms(from, to);
                    return UndoMoveInfo.of(move, p, currentPerms);
                }
                case Move.FLAG_PROMOTE_Q, Move.FLAG_PROMOTE_Q_CAPTURE -> {
                    int p = pieceMap[from];                                 //  remove the pawn
                    clearOnBoard(p, from);                                  // ------------------
                    int placedP = Piece.of(Piece.colorInt(p), Piece.QUEEN);
                    PlaceOnBoard(placedP, from);                            // place a Queen on the from-pos
                    pieceMap[from] = (byte) placedP;                        // also replace it in the pieceMap so that after the move below the Queen is in the correct pos
                    // example:
                    // WPawn (b7); BRook (a8)
                    // delete Pawn -> replace with Queen of same color
                    // now the movePiece below will capture the Rook with the queen and the pos will be correct again.
                }
                case Move.FLAG_PROMOTE_R, Move.FLAG_PROMOTE_R_CAPTURE -> {
                    //  remove the pawn
                    int p = pieceMap[from];
                    clearOnBoard(p, from);
                    int placedP = Piece.of(Piece.colorInt(p), Piece.ROOK);
                    PlaceOnBoard(placedP, from);
                    pieceMap[from] = (byte) placedP;
                }
                case Move.FLAG_PROMOTE_B, Move.FLAG_PROMOTE_B_CAPTURE -> {
                    //  remove the pawn
                    int p = pieceMap[from];
                    clearOnBoard(p, from);
                    int placedP = Piece.of(Piece.colorInt(p), Piece.BISHOP);
                    PlaceOnBoard(placedP, from);
                    pieceMap[from] = (byte) placedP;
                }
                case Move.FLAG_PROMOTE_N, Move.FLAG_PROMOTE_N_CAPTURE -> {
                    //  remove the pawn
                    int p = pieceMap[from];
                    clearOnBoard(p, from);
                    int placedP = Piece.of(Piece.colorInt(p), Piece.KNIGHT);
                    PlaceOnBoard(placedP, from);
                    pieceMap[from] = (byte) placedP;
                }

                default -> {

                }
            }

            byte currentPerms = updateCastlePerms(from, to);
            return UndoMoveInfo.of(move, movePiece(from, to), currentPerms);
        }

        public void unmakeMove(int undoInfo) {
            int move = UndoMoveInfo.move(undoInfo);
            int from      = Move.from(move);
            int to        = Move.to(move);
            int flags = Move.flags(move);
            boolean fAttack = Move.attackFromFlag(flags);
            int fType = Move.baseFlag(flags);
            int takenPiece = UndoMoveInfo.takenPiece(undoInfo);
            int colorOfMovedPiece = Piece.colorInt(pieceMap[to]);

            // only check baseType ignore attack bit
            switch (fType){
                case Move.FLAG_CASTLE_K -> {
                    // move King back
                    movePiece(to, from);
                    // rook left of the king -> to the corner
                    movePiece(to - 1, from + (distRight(from)));
                }
                case Move.FLAG_CASTLE_Q -> {
                    // move King back
                    movePiece(to, from);
                    // rook one right of the king -> to the corner
                    movePiece(to + 1, from - (distLeft(from)));
                }
                case Move.FLAG_EN_PASSANT -> {
                    // returned taking-pawn to orig. pos
                    movePiece(to, from);

                    // get the offset to the pawnField
                    int dir = Integer.compare(from, to) * 8;
                    PlaceOnBoard(takenPiece,to + dir);
                    pieceMap[to+dir] = (byte) takenPiece;

                }
                case Move.FLAG_PROMOTE_Q, Move.FLAG_PROMOTE_R, Move.FLAG_PROMOTE_B, Move.FLAG_PROMOTE_N  -> {
                    clearOnBoard(pieceMap[to], to);                                 // remove the promoted piece
                    int pawn = Piece.of(colorOfMovedPiece, Piece.PAWN);
                    PlaceOnBoard(pawn , from);                                      // place the original pawn
                    PlaceOnBoard(takenPiece, to);                                   // place the TakenPiece
                    pieceMap[to] = (byte) takenPiece;
                    pieceMap[from] = (byte) pawn;
                }
                case Move.FLAG_GENERIC -> {
                    movePiece(to, from);
                    PlaceOnBoard(takenPiece, to);     // sets takenPiece -> if emptyPiece nothing is done;
                    pieceMap[to] = (byte) takenPiece;
                }
            }

            resetCastlePerms(UndoMoveInfo.castlePerms(undoInfo));
            updateHelper();
            calcMat();
            whiteToMove = !whiteToMove;
            fiftyMoveCounter --;
        }

        /**
         * checks if the called color is in checkmate
         * <p>
         * if called with white (true) and white is in Checkmate it will return GameState.Checkmate
         */
        public GameState getState(boolean color){
            boolean isChecked = checkChess(color);
            boolean hasMoves = Check.hasMoves(this, whiteToMove);

            if(fiftyMoveCounter > 100){
                return GameState.FiftyMove;
            }

            if (isChecked){
                if(hasMoves){
                    return GameState.Continue;
                }
                return GameState.CheckMate;
            }
            if(hasMoves){
                return GameState.Continue;
            }
            return GameState.StaleMate;
        }

        // Check detection

        public boolean checkChess(boolean color) {
            return color
                    ? checkChess(Bitboard.lsb(WKing), true)         // lsb returned das square
                    : checkChess(Bitboard.lsb(BKing), false);
        }

        private boolean checkChess(int kingPos, boolean kingColor) {
            int kingRow = kingPos / SIZE;           // get Row
            int kingCol = kingPos % SIZE;           // get Col

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

        /**
         * moves a piece and keeps the pieceMap in sync
         * @param from start-location
         * @param to end-location
         * @return taken-piece (can be an Empty-piece)
         */
        private int movePiece(int from, int to) {
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
            return takenPiece;
        }

        /**
         * places a piece on a square at the bitboard level
         * pieceMap isn't affected
         * @param piece pieceT with color
         * @param sq square to place on
         */
        private void PlaceOnBoard(int piece, int sq){
            boolean color = Piece.color(piece);
            int type      = Piece.pieceT(piece);

            if (color) { // white
                switch (type) {
                    case Piece.EMPTY_PIECE -> {}
                    case Piece.PAWN -> WPawns = Bitboard.set(WPawns, sq);
                    case Piece.KNIGHT -> WKnights = Bitboard.set(WKnights, sq);
                    case Piece.BISHOP -> WBishops = Bitboard.set(WBishops, sq);
                    case Piece.ROOK -> WRooks = Bitboard.set(WRooks, sq);
                    case Piece.QUEEN -> WQueens = Bitboard.set(WQueens, sq);
                    case Piece.KING -> WKing = Bitboard.set(WKing, sq);
                    default -> throw new InvalidPieceTypeException("PlaceOnBoard: unknown white piece type " + type);
                }
            }else {
                switch (type) {
                    case Piece.EMPTY_PIECE -> {}
                    case Piece.PAWN -> BPawns = Bitboard.set(BPawns, sq);
                    case Piece.KNIGHT -> BKnights = Bitboard.set(BKnights, sq);
                    case Piece.BISHOP -> BBishops = Bitboard.set(BBishops, sq);
                    case Piece.ROOK -> BRooks = Bitboard.set(BRooks, sq);
                    case Piece.QUEEN -> BQueens = Bitboard.set(BQueens, sq);
                    case Piece.KING -> BKing = Bitboard.set(BKing, sq);
                    default -> throw new InvalidPieceTypeException("PlaceOnBoard: unknown white piece type " + type);
                }
            }
        }

        /**
         * Moves a piece on its corresponding bitboard (clear from, set to).
         * Looks up the board by piece byte and reassigns the field.
         *
         * doesn't affect pieceMap
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

        /**
         * Clears a square on the bitboard corresponding to the given piece byte. <p>
         * Also clears the pieceMap
         * @param piece pieceT and color
         * @param sq square
         */
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
            pieceMap[sq] = 0;
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

        public void updateHelper() {
            WPieces  = WPawns | WKnights | WBishops | WRooks | WQueens | WKing;
            BPieces  = BPawns | BKnights | BBishops | BRooks | BQueens | BKing;
            Occupied = WPieces | BPieces;
        }

        public void calcMat() {
            whiteMaterial =
                            Long.bitCount(WPawns)   * pieceVals[Piece.PAWN]
                            + Long.bitCount(WKnights) * pieceVals[Piece.KNIGHT]
                            + Long.bitCount(WBishops) * pieceVals[Piece.BISHOP]
                            + Long.bitCount(WRooks)   * pieceVals[Piece.ROOK]
                            + Long.bitCount(WQueens)  * pieceVals[Piece.QUEEN]
                            + Long.bitCount(WKing)    * pieceVals[Piece.KING];

            blackMaterial =
                            Long.bitCount(BPawns)   * pieceVals[Piece.PAWN]
                            + Long.bitCount(BKnights) * pieceVals[Piece.KNIGHT]
                            + Long.bitCount(BBishops) * pieceVals[Piece.BISHOP]
                            + Long.bitCount(BRooks)   * pieceVals[Piece.ROOK]
                            + Long.bitCount(BQueens)  * pieceVals[Piece.QUEEN]
                            + Long.bitCount(BKing)    * pieceVals[Piece.KING];
        }

        private byte updateCastlePerms(int from, int to){
            byte prevPerms = castlePerms;
            // these squares are fixed by the rules of chess
            if (from == E1 || to == E1) castlePerms &= ~(W_KINGSIDE | W_QUEENSIDE);
            if (from == H1 || to == H1) castlePerms &= ~W_KINGSIDE;
            if (from == A1 || to == A1) castlePerms &= ~W_QUEENSIDE;
            if (from == E8 || to == E8) castlePerms &= ~(B_KINGSIDE | B_QUEENSIDE);
            if (from == H8 || to == H8) castlePerms &= ~B_KINGSIDE;
            if (from == A8 || to == A8) castlePerms &= ~B_QUEENSIDE;
            return prevPerms;
        }

        private void resetCastlePerms(byte perms){
            castlePerms = perms;
        }


        public void revokeCastlePerms(byte Perm){
            castlePerms &= (byte) ~Perm;
        }

        public boolean queryCastlePerms(byte Perm){
            return (castlePerms & Perm) != 0;
        }

        /** Empty position — no pieces, clean state. */
        public static Position emptyPosition() {
            Position pos = new Position();
            Arrays.fill(pos.pieceMap, (byte) Piece.EMPTY_PIECE);
            return pos;
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

            p.Occupied = 0xFFFF00000000FFFFL;
            p.WPieces  = 0x000000000000FFFFL;
            p.BPieces  = 0xFFFF000000000000L;

            // materials
            p.whiteMaterial =  39;
            p.blackMaterial =  39;

            // game-state castling
            p.WKingCastle  = false;
            p.WQueenCastle = false;
            p.BKingCastle  = false;
            p.BQueenCastle = false;

            // castle-perms all true
            p.castlePerms = 0b1111;

            // White pieces
            p.pieceMap[0] = (byte) Piece.of(Piece.WHITE, Piece.ROOK);
            p.pieceMap[1] = (byte) Piece.of(Piece.WHITE, Piece.KNIGHT);
            p.pieceMap[2] = (byte) Piece.of(Piece.WHITE, Piece.BISHOP);
            p.pieceMap[3] = (byte) Piece.of(Piece.WHITE, Piece.QUEEN);
            p.pieceMap[4] = (byte) Piece.of(Piece.WHITE, Piece.KING);
            p.pieceMap[5] = (byte) Piece.of(Piece.WHITE, Piece.BISHOP);
            p.pieceMap[6] = (byte) Piece.of(Piece.WHITE, Piece.KNIGHT);
            p.pieceMap[7] = (byte) Piece.of(Piece.WHITE, Piece.ROOK);

            for (int i = 8; i < 16; i++) {
                p.pieceMap[i] = (byte) Piece.of(Piece.WHITE, Piece.PAWN);
            }

            // Black pieces
            p.pieceMap[56] = (byte) Piece.of(Piece.BLACK, Piece.ROOK);
            p.pieceMap[57] = (byte) Piece.of(Piece.BLACK, Piece.KNIGHT);
            p.pieceMap[58] = (byte) Piece.of(Piece.BLACK, Piece.BISHOP);
            p.pieceMap[59] = (byte) Piece.of(Piece.BLACK, Piece.QUEEN);   // d8
            p.pieceMap[60] = (byte) Piece.of(Piece.BLACK, Piece.KING);  // e8
            p.pieceMap[61] = (byte) Piece.of(Piece.BLACK, Piece.BISHOP);
            p.pieceMap[62] = (byte) Piece.of(Piece.BLACK, Piece.KNIGHT);
            p.pieceMap[63] = (byte) Piece.of(Piece.BLACK, Piece.ROOK);

            for (int i = 48; i < 56; i++) {
                p.pieceMap[i] = (byte) Piece.of(Piece.BLACK, Piece.PAWN);
            }

            return p;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return WPawns == position.WPawns && WKnights == position.WKnights && WBishops == position.WBishops && WRooks == position.WRooks && WQueens == position.WQueens && WKing == position.WKing && BPawns == position.BPawns && BKnights == position.BKnights && BBishops == position.BBishops && BRooks == position.BRooks && BQueens == position.BQueens && BKing == position.BKing && WDoublePawnMove == position.WDoublePawnMove && BDoublePawnMove == position.BDoublePawnMove && Occupied == position.Occupied && WPieces == position.WPieces && BPieces == position.BPieces && castlePerms == position.castlePerms && WQueenCastle == position.WQueenCastle && WKingCastle == position.WKingCastle && BQueenCastle == position.BQueenCastle && BKingCastle == position.BKingCastle && enPassantSquare == position.enPassantSquare && whiteToMove == position.whiteToMove && fiftyMoveCounter == position.fiftyMoveCounter && whiteMaterial == position.whiteMaterial && blackMaterial == position.blackMaterial && Objects.deepEquals(pieceMap, position.pieceMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(WPawns, WKnights, WBishops, WRooks, WQueens, WKing, BPawns, BKnights, BBishops, BRooks, BQueens, BKing, WDoublePawnMove, BDoublePawnMove, Occupied, WPieces, BPieces, castlePerms, WQueenCastle, WKingCastle, BQueenCastle, BKingCastle, enPassantSquare, whiteToMove, fiftyMoveCounter, Arrays.hashCode(pieceMap), whiteMaterial, blackMaterial);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Position{");
            sb.append("WPawns=").append(WPawns);
            sb.append(", WKnights=").append(WKnights);
            sb.append(", WBishops=").append(WBishops);
            sb.append(", WRooks=").append(WRooks);
            sb.append(", WQueens=").append(WQueens);
            sb.append(", WKing=").append(WKing);
            sb.append(", BPawns=").append(BPawns);
            sb.append(", BKnights=").append(BKnights);
            sb.append(", BBishops=").append(BBishops);
            sb.append(", BRooks=").append(BRooks);
            sb.append(", BQueens=").append(BQueens);
            sb.append(", BKing=").append(BKing);
            sb.append(", WDoublePawnMove=").append(WDoublePawnMove);
            sb.append(", BDoublePawnMove=").append(BDoublePawnMove);
            sb.append(", Occupied=").append(Occupied);
            sb.append(", WPieces=").append(WPieces);
            sb.append(", BPieces=").append(BPieces);
            sb.append(", castlePerms=").append(castlePerms);
            sb.append(", WQueenCastle=").append(WQueenCastle);
            sb.append(", WKingCastle=").append(WKingCastle);
            sb.append(", BQueenCastle=").append(BQueenCastle);
            sb.append(", BKingCastle=").append(BKingCastle);
            sb.append(", enPassantSquare=").append(enPassantSquare);
            sb.append(", whiteToMove=").append(whiteToMove);
            sb.append(", fiftyMoveCounter=").append(fiftyMoveCounter);
            sb.append(", pieceMap=").append(Arrays.toString(pieceMap));
            sb.append(", whiteMaterial=").append(whiteMaterial);
            sb.append(", blackMaterial=").append(blackMaterial);
            sb.append('}');
            return sb.toString();
        }
    }


    public static void main(String[] args) {
        Position pos = Position.StartingPosition();
        System.out.println(pos.Occupied);
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

    }
}