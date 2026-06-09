package org.mxnik.forcechess.Pos;

import org.mxnik.forcechess.General.Bitboard;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Moves.GameState;

import static org.mxnik.forcechess.Pos.PositionEncoder.Position.*;

public class MoveGen {

    /**
     * generate all moves and the gamestate of the current position
     * @param pos   the given position
     * @param whiteToMove   whoose turn. (important for check's etc)
     * @param moves         the int[] that the move will be in
     * @return A pair of the movecount and the GameState
     */
    public static DiversePair <Integer, GameState> generateMovesAndResult(PositionEncoder.Position pos, boolean whiteToMove, int[] moves) {
        int newOff = generateMoves(pos, 0, whiteToMove, moves);
        if(newOff == 0){                                            // no new moves
            boolean check = pos.checkChess(!whiteToMove);
            return check ? new DiversePair<>(newOff, GameState.CheckMate) : new DiversePair<>(newOff, GameState.StaleMate);
        }
        return new DiversePair<>(newOff, GameState.Continue);
    }

    /**
     * Generates all legal moves for the side to move.
     *
     *
     * @param pos current game-state
     * @param offset   start index in the move array
     * @param moves    pre-allocated move array (max 256 * search depth)
     * @param whiteToMove passed separately for testing purposes
     * @return new offset
     */
    public static int generateMoves(PositionEncoder.Position pos, int offset, boolean whiteToMove, int[] moves) {
        int pseudoOffset = generatePseudoMoves(pos, offset, whiteToMove, moves);
        int legalOffset = pseudoOffset;

        int place = offset;                                 // where next move should be places ( after illegal move -> rearrange moves)
        for (int i = offset; i < pseudoOffset; i++) {
            int move = moves[i];
            int undo = pos.makeMove(moves[i]);


            if (pos.checkChess(whiteToMove)){
                //System.out.println("check detected");
                legalOffset --;                            // one less move

                pos.unmakeMove(undo);
                continue;                                   // don't add move
            }

            moves[place] = moves[i];                        // place the move at the correct offset
            place ++;
            pos.unmakeMove(undo);
        }
        return legalOffset;
    }


    /**
     * Generates all possible moves for the side to move.
     * Doesn't check for legal moves (king in check)
     *
     * @param position current gamestate
     * @param offset   start index in the move array
     * @param moves    pre-allocated move array (max 256 * search depth)
     * @return new offset
     */
    public static int generatePseudoMoves(PositionEncoder.Position position, int offset, boolean whiteToMove, int[] moves) {
        if (whiteToMove) return generateMovesW(position, offset, moves);
        return generateMovesB(position, offset, moves);
    }

    // White

    /**
     * generate all moves for white.
     */
    private static int generateMovesW(PositionEncoder.Position pos, int offset, int[] moves) {
        // Pawn pushes — can't land on any occupied square
        long singleP = (pos.WPawns << PositionEncoder.SIZE) & ~pos.Occupied;
        // log. and with single push because every double push has to also have a single push avail. if not then pawn + firstRank = blocked
        long doubleP = (((pos.WPawns & pos.WDoublePawnMove) << PositionEncoder.SIZE * 2) & ~pos.Occupied) & singleP << PositionEncoder.SIZE;


        // Pawn attacks — must land on enemy square
        // mask the file on the opposite side
        long attackL = ((pos.WPawns << PositionEncoder.SIZE - 1) & pos.BPieces) & ~Move.FILE_H;
        long attackR = ((pos.WPawns << PositionEncoder.SIZE + 1) & pos.BPieces) & ~Move.FILE_A;

        // En passant attacks

        long enPassant = 1L << pos.enPassantSquare;
        if(pos.enPassantSquare == -1)
            enPassant = 0;          // set 0 for no enPassant (-1 will wrap to 63)


        long enPassantL = (pos.WPawns << PositionEncoder.SIZE - 1) & enPassant;
        long enPassantR = (pos.WPawns << PositionEncoder.SIZE + 1) & enPassant;

        // Promotion
        // currently only single pushes to promotion might add double later
        long promotion = singleP & Move.ROW_8;                                      // any pawns landing on the last row can be promoted
        long attackPromotionL = attackL & Move.ROW_8;
        long attackPromotionR = attackR & Move.ROW_8;

        // mask the normal pushes with 8'th row. can't choose to not promote
        singleP &= ~Move.ROW_8;
        doubleP &= ~Move.ROW_8;
        attackL &= ~Move.ROW_8;
        attackR &= ~Move.ROW_8;

        // norm. moves
        offset = formatPawnMoves(singleP,     PositionEncoder.SIZE,     offset, Move.FLAG_GENERIC, moves);
        offset = formatPawnMoves(doubleP,     PositionEncoder.SIZE * 2, offset, Move.FLAG_GENERIC, moves);
        offset = formatPawnMoves(attackL,     PositionEncoder.SIZE - 1, offset, Move.FLAG_GENERIC_CAPTURE, moves);
        offset = formatPawnMoves(attackR,     PositionEncoder.SIZE + 1, offset, Move.FLAG_GENERIC_CAPTURE, moves);
        // en-passant
        offset = formatPawnMoves(enPassantL,  PositionEncoder.SIZE - 1, offset, Move.FLAG_EN_PASSANT_CAPTURE, moves);
        offset = formatPawnMoves(enPassantR,  PositionEncoder.SIZE + 1, offset, Move.FLAG_EN_PASSANT_CAPTURE, moves);
        // promotions (no capture) 4 unique poss. Rook, Queen, Bishop, Knight
        offset = formatPawnMoves(promotion,  PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_Q, moves);
        offset = formatPawnMoves(promotion,  PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_R, moves);
        offset = formatPawnMoves(promotion,  PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_B, moves);
        offset = formatPawnMoves(promotion,  PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_N, moves);
        // promotions (capture) 8 unique poss. (Rook, Queen, Bishop, Knight) both dir.
        // left capture
        offset = formatPawnMoves(attackPromotionL,  PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_Q_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionL,  PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_R_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionL,  PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_B_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionL,  PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_N_CAPTURE, moves);
        // right capture
        offset = formatPawnMoves(attackPromotionR,  PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_Q_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionR,  PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_R_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionR,  PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_B_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionR,  PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_N_CAPTURE, moves);


        // Knights
        long knights = pos.WKnights;
        while (!Bitboard.isEmpty(knights)) {
            int sq = Bitboard.lsb(knights);
            knights = Bitboard.popLsb(knights);
            offset = drainBitboard(pos.WPieces, pos.BPieces, Move.KNIGHT_LOOKUP[sq], sq, offset, moves);
        }

        // Bishops
        long bishops = pos.WBishops;
        while (!Bitboard.isEmpty(bishops)) {
            int sq = Bitboard.lsb(bishops);
            bishops = Bitboard.popLsb(bishops);
            offset = drainBitboard(pos.WPieces, pos.BPieces, bishopMoves(sq, pos.Occupied, pos.WPieces), sq, offset, moves);
        }

        // Rooks
        long rooks = pos.WRooks;
        while (!Bitboard.isEmpty(rooks)) {
            int sq = Bitboard.lsb(rooks);
            rooks = Bitboard.popLsb(rooks);
            offset = drainBitboard(pos.WPieces, pos.BPieces, rookMoves(sq, pos.Occupied, pos.WPieces), sq, offset, moves);
        }

        // Queens
        long queens = pos.WQueens;
        while (!Bitboard.isEmpty(queens)) {
            int sq = Bitboard.lsb(queens);
            queens = Bitboard.popLsb(queens);
            offset = drainBitboard(pos.WPieces, pos.BPieces, queenMoves(sq, pos.Occupied, pos.WPieces), sq, offset, moves);
        }

        // King
        long king = pos.WKing;
        while (!Bitboard.isEmpty(king)) {
            int sq = Bitboard.lsb(king);
            king = Bitboard.popLsb(king);
            offset = drainBitboard(pos.WPieces, pos.BPieces, Move.KING_LOOKUP[sq], sq, offset, moves);
        }

        // CASTLES:::::: YIPPEE
        if(pos.checkChess(true)){
            return offset;
        }

        if (pos.queryCastlePerms(W_KINGSIDE)){
            pos.WKingCastle = true;
            int sq = Bitboard.lsb(pos.WKing);
            for (int i = sq+1; i < 7; i++) {                   // iterate from the kingPos + 1 to the rookPos - 1
                if (((pos.Occupied >> i) & 0x1) == 1L){       // a piece is blocking the way
                    pos.WKingCastle = false;
                    break;
                }
                if(pos.checkChess(i, pos.whiteToMove)){
                    pos.WKingCastle = false;
                    break;
                }
            }

            if (pos.WKingCastle){
                moves[offset++] = Move.of(sq, sq+2, Move.FLAG_CASTLE_K);
            }
        }

        if (pos.queryCastlePerms(W_QUEENSIDE)){
            pos.WQueenCastle = true;
            int sq = Bitboard.lsb(pos.WKing);
            for (int i = sq-1; i > 0; i--) {                   // iterate from the kingPos + 1 to the rookPos - 1
                if (((pos.Occupied >> i) & 0x1) == 1L){       // a piece is blocking the way
                    pos.WQueenCastle = false;
                    break;
                }
                if(pos.checkChess(i, pos.whiteToMove)){
                    pos.WQueenCastle = false;
                    break;
                }
            }

            if (pos.WQueenCastle){
                moves[offset++] = Move.of(sq, sq-2, Move.FLAG_CASTLE_Q);
            }
        }


        return offset;
    }


    // Black

    /**
     * Generate all moves for Black
     */
    private static int generateMovesB(PositionEncoder.Position pos, int offset, int[] moves) {
        // Pawn pushes — can't land on any occupied square
        long singleP = (pos.BPawns >>> PositionEncoder.SIZE) & ~pos.Occupied;
        // log. and with single push because every double push has to also have a single push avail. if not then pawn + firstRank = blocked
        long doubleP = (((pos.BPawns & pos.BDoublePawnMove) >>> PositionEncoder.SIZE * 2) & ~pos.Occupied) & singleP >>> PositionEncoder.SIZE;


        // Pawn attacks — must land on enemy square
        long attackL = ((pos.BPawns >>> PositionEncoder.SIZE + 1) & pos.WPieces) & ~Move.FILE_H;
        long attackR = ((pos.BPawns >>> PositionEncoder.SIZE - 1) & pos.WPieces) & ~Move.FILE_A;

        // En passant attacks
        long enPassant =  1L << pos.enPassantSquare;

        if(pos.enPassantSquare == -1)
            enPassant = 0;          // set 0 for no enPassant (-1 will wrap to 63)

        long enPassantL = (pos.BPawns >>> PositionEncoder.SIZE + 1) & enPassant;
        long enPassantR = (pos.BPawns >>> PositionEncoder.SIZE - 1) & enPassant;

        // Promotion
        // currently only single pushes to promotion might add double later
        long promotion = singleP & Move.ROW_1;                                      // any pawns landing on the last row can be promoted
        long attackPromotionL = attackL & Move.ROW_1;
        long attackPromotionR = attackR & Move.ROW_1;

        // mask the normal pushes with 8'th row. can't choose to not promote
        singleP &= ~Move.ROW_1;
        doubleP &= ~Move.ROW_1;
        attackL &= ~Move.ROW_1;
        attackR &= ~Move.ROW_1;

        offset = formatPawnMoves(singleP,     -PositionEncoder.SIZE,     offset, Move.FLAG_GENERIC, moves);
        offset = formatPawnMoves(doubleP,     -PositionEncoder.SIZE * 2, offset, Move.FLAG_GENERIC, moves);
        offset = formatPawnMoves(attackL,     -(PositionEncoder.SIZE + 1), offset, Move.FLAG_GENERIC_CAPTURE, moves);
        offset = formatPawnMoves(attackR,     -(PositionEncoder.SIZE - 1), offset, Move.FLAG_GENERIC_CAPTURE, moves);
        offset = formatPawnMoves(enPassantL,  -(PositionEncoder.SIZE + 1), offset, Move.FLAG_EN_PASSANT_CAPTURE, moves);
        offset = formatPawnMoves(enPassantR,  -(PositionEncoder.SIZE - 1), offset, Move.FLAG_EN_PASSANT_CAPTURE, moves);
        // promotions (no capture) 4 unique poss. Rook, Queen, Bishop, Knight
        offset = formatPawnMoves(promotion,  -PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_Q, moves);
        offset = formatPawnMoves(promotion,  -PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_R, moves);
        offset = formatPawnMoves(promotion,  -PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_B, moves);
        offset = formatPawnMoves(promotion,  -PositionEncoder.SIZE, offset, Move.FLAG_PROMOTE_N, moves);
        // promotions (capture) 8 unique poss. (Rook, Queen, Bishop, Knight) both dir.
        // right capture
        offset = formatPawnMoves(attackPromotionL,  -PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_Q_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionL,  -PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_R_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionL,  -PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_B_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionL,  -PositionEncoder.SIZE - 1, offset, Move.FLAG_PROMOTE_N_CAPTURE, moves);
        // left capture
        offset = formatPawnMoves(attackPromotionR,  -PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_Q_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionR,  -PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_R_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionR,  -PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_B_CAPTURE, moves);
        offset = formatPawnMoves(attackPromotionR,  -PositionEncoder.SIZE + 1, offset, Move.FLAG_PROMOTE_N_CAPTURE, moves);

        // Knights
        long knights = pos.BKnights;
        while (!Bitboard.isEmpty(knights)) {
            int sq = Bitboard.lsb(knights);
            knights = Bitboard.popLsb(knights);
            offset = drainBitboard(pos.BPieces, pos.WPieces, Move.KNIGHT_LOOKUP[sq], sq, offset, moves);
        }

        // Bishops
        long bishops = pos.BBishops;
        while (!Bitboard.isEmpty(bishops)) {
            int sq = Bitboard.lsb(bishops);
            bishops = Bitboard.popLsb(bishops);
            offset = drainBitboard(pos.BPieces, pos.WPieces, bishopMoves(sq, pos.Occupied, pos.BPieces), sq, offset, moves);
        }

        // Rooks
        long rooks = pos.BRooks;
        while (!Bitboard.isEmpty(rooks)) {
            int sq = Bitboard.lsb(rooks);
            rooks = Bitboard.popLsb(rooks);
            offset = drainBitboard(pos.BPieces, pos.WPieces, rookMoves(sq, pos.Occupied, pos.BPieces), sq, offset, moves);
        }

        // Queens
        long queens = pos.BQueens;
        while (!Bitboard.isEmpty(queens)) {
            int sq = Bitboard.lsb(queens);
            queens = Bitboard.popLsb(queens);
            offset = drainBitboard(pos.BPieces, pos.WPieces, queenMoves(sq, pos.Occupied, pos.BPieces), sq, offset, moves);
        }

        // King
        long king = pos.BKing;
        while (!Bitboard.isEmpty(king)) {
            int sq = Bitboard.lsb(king);
            king = Bitboard.popLsb(king);
            offset = drainBitboard(pos.BPieces, pos.WPieces, Move.KING_LOOKUP[sq], sq, offset, moves);
        }

        // CASTLES:::::: YIPPEE
        if(pos.checkChess(false)){
            return offset;
        }

        if (pos.queryCastlePerms(B_KINGSIDE)){
            pos.BKingCastle = true;
            int sq = Bitboard.lsb(pos.BKing);
            for (int i = sq+1; i < 63; i++) {                 // iterate from the kingPos + 1 to the rookPos - 1
                if (((pos.Occupied >> i) & 0x1) == 1L){       // a piece is blocking the way
                    pos.BKingCastle = false;
                    break;
                }
                if(pos.checkChess(i, pos.whiteToMove)){
                    pos.BKingCastle = false;
                    break;
                }
            }

            if (pos.BKingCastle){
                moves[offset++] = Move.of(sq, sq+2, Move.FLAG_CASTLE_K);
            }
        }

        if (pos.queryCastlePerms(B_QUEENSIDE)){
            pos.BQueenCastle = true;
            int sq = Bitboard.lsb(pos.BKing);
            for (int i = sq-1; i > 56; i--) {                 // iterate from the kingPos + 1 to the rookPos - 1
                if (((pos.Occupied >> i) & 0x1) == 1L){       // a piece is blocking the way
                    pos.BQueenCastle = false;
                    break;
                }
                if(pos.checkChess(i, pos.whiteToMove)){
                    pos.BQueenCastle = false;
                    break;
                }
            }

            if (pos.BQueenCastle){
                moves[offset++] = Move.of(sq, sq-2, Move.FLAG_CASTLE_Q);
            }
        }

        return offset;
    }

    // Slide move helpers

    /**
     * combinatino of bishop + rookmoves
     */
    static long queenMoves(int square, long occupied, long ownPieces) {
        return rookMoves(square, occupied, ownPieces)
             | bishopMoves(square, occupied, ownPieces);
    }

    /**
     * generate all moves a rook has starting at square.
     * @param occupied all piece position in a bitmap
     * @param ownPieces all pieces of your color in a bitmap
     */
    static long rookMoves(int square, long occupied, long ownPieces) {
        return slideAttacks(square,  PositionEncoder.SIZE, occupied, ownPieces, Move.ROW_1)  // up
             | slideAttacks(square, -PositionEncoder.SIZE, occupied, ownPieces, Move.ROW_8)  // down
             | slideAttacks(square, -1, occupied, ownPieces, Move.FILE_H)              // left
             | slideAttacks(square,  1, occupied, ownPieces, Move.FILE_A);             // right
    }


    /**
     * generate all moves a bishop has starting at square.
     * @param occupied all piece position in a bitmap
     * @param ownPieces all pieces of your color in a bitmap
     */
    static long bishopMoves(int square, long occupied, long ownPieces) {
        return slideAttacks(square,  PositionEncoder.SIZE + 1, occupied, ownPieces, Move.FILE_A | Move.ROW_1)  // right-up
             | slideAttacks(square,  PositionEncoder.SIZE - 1, occupied, ownPieces, Move.FILE_H | Move.ROW_1)  // left-up
             | slideAttacks(square, -PositionEncoder.SIZE + 1, occupied, ownPieces, Move.FILE_A | Move.ROW_8)  // right-down
             | slideAttacks(square, -PositionEncoder.SIZE - 1, occupied, ownPieces, Move.FILE_H | Move.ROW_8); // left-down
    }

    /**
     * Slides in one direction until the edge of the board or a collision.
     * Captures on enemy pieces are included; own-piece collisions are stripped
     * in drainBitboard, not here, so the caller can distinguish captures.
     *
     * @param square starting square
     * @param delta offset to move
     * @param occupied blockers (own and enemyPieces)
     * @param ownPieces (same colored pieces)
     * @param border fields that cannot be accessed <p> example: rook moving upward (+8) <p> bottom row is border
     */
    private static long slideAttacks(int square, int delta, long occupied, long ownPieces, long border) {
        long attack = 0L;
        long b = 1L << square;

        while (true) {
            if(delta > 0) {
                b <<= delta;
            }else {
                // flip to positive and shift logically
                b >>>= -delta;
            }
            // check with inverted border
            b &= ~border;
            if (b == 0L) break;   // fell off the board

            attack |= b;

            if ((b & occupied) != 0L) break; // piece collision — include square, stop ray
        }

        attack &= ~ownPieces; // strip own-piece collisions
        return attack;
    }

    // Move array helpers

    /**
     * Drains a bitboard of end-squares into the move array,
     * tagging each move as capture or quiet based on enemy occupancy.
     */
    private static int drainBitboard(long ownPieces, long enemyPieces, long endPositions,
                                     int startSq, int offset, int[] moves) {
        endPositions &= ~ownPieces; // never land on own pieces

        while (endPositions != 0L) {
            int sq = Bitboard.lsb(endPositions);
            endPositions = Bitboard.popLsb(endPositions);

            int flag = ((enemyPieces >>> sq) & 1L) == 1
                ? Move.FLAG_GENERIC_CAPTURE
                : Move.FLAG_GENERIC;

            moves[offset++] = Move.of(startSq, sq, flag);
        }
        return offset;
    }

    /**
     * Converts a bitboard of pawn destination squares into move entries.
     * The source square is recovered by subtracting the move offset.
     */
    private static int formatPawnMoves(long bitBoard, int moveOffset, int offset,
                                       int flags, int[] moves) {
        while (bitBoard != 0L) {
            int sq = Bitboard.lsb(bitBoard);
            bitBoard = Bitboard.popLsb(bitBoard);
            moves[offset++] = Move.of(sq - moveOffset, sq, flags);
        }
        return offset;
    }
}
