package org.mxnik.forcechess.bot.baseStateBot;

public class MoveGen {
    /**
     * Generates all possible moves for the side to move
     * @param position current gamestate
     * @param offset start in the move array
     * @param moves all moves from top of MCTS to bottom depth max 256 * max search depth
     * @return new offset
     */
    public static int generateMoves (PositionEncoder.Position position, int offset, boolean whiteToMove, int[] moves){
        //TODO: add Tests
        if (whiteToMove){
            return generateMovesW(position, offset, moves);
        }
        return generateMovesB(position, offset, moves);
    }


    /**
     * Generates all moves for the BLack side
     * @param pos the current positions / gamestate
     * @param offset offset to start in move arr
     * @param moves all moves till full depth
     * @return new offset into moves arr
     */
    private static int generateMovesB (PositionEncoder.Position pos, int offset, int[] moves){
        // Black Pawn move gen
        // single & double pushes can't take anything
        long singleP = (pos.BPawns.board << PositionEncoder.SIZE) & ~pos.Occupied;
        long doubleP = ((pos.BPawns.board & pos.BDoublePawnMove.board) << PositionEncoder.SIZE * 2)  & ~pos.Occupied;
        // check if field is occupied (by enemy) when trying to take
        long attackL = (pos.BPawns.board << PositionEncoder.SIZE - 1) & pos.WPieces;
        long attackR = (pos.BPawns.board << PositionEncoder.SIZE + 1) & pos.WPieces;

        // check if any attack fields land on the enPassant square
        long enPassantL = (pos.BPawns.board << PositionEncoder.SIZE - 1)
                & (pos.enPassant);
        long enPassantR = (pos.BPawns.board << PositionEncoder.SIZE + 1)
                & (pos.enPassant);

        // gen actual moves (add to move array)
        offset = formatPawnMoves(singleP, PositionEncoder.SIZE, offset, Move.FLAG_GENERIC, Move.BLACK, moves);
        offset = formatPawnMoves(doubleP, PositionEncoder.SIZE * 2, offset, Move.FLAG_GENERIC, Move.BLACK, moves);
        offset = formatPawnMoves(attackL, PositionEncoder.SIZE - 1, offset, Move.FLAG_CAPTURE, Move.BLACK, moves);
        offset = formatPawnMoves(attackR, PositionEncoder.SIZE + 1, offset, Move.FLAG_CAPTURE, Move.BLACK, moves);
        offset = formatPawnMoves(enPassantL, PositionEncoder.SIZE - 1, offset, Move.FLAG_EN_PASSANT, Move.BLACK, moves);
        offset = formatPawnMoves(enPassantR, PositionEncoder.SIZE + 1, offset, Move.FLAG_EN_PASSANT, Move.BLACK, moves);

        // Black Pawn end

        // Black Knight move gen
        while (!pos.BKnights.isEmpty()){
            int startSq = pos.BKnights.popLsb();
            //get all end positions possible from startSq
            long endPositions = Move.KNIGHT_LOOKUP[startSq];
            offset = drainBitboard(pos.BPieces, pos.WPieces, endPositions, startSq, Move.BLACK, Move.QUEEN, offset, moves);
        }

        // Black Knight end

        // Black-Bishops
        int sq;
        long endPositions;
        while (!pos.BBishops.isEmpty()){
            sq = pos.BBishops.popLsb();
            endPositions = BishopMoves(sq, pos.Occupied, pos.BPieces);
            offset = drainBitboard(pos.BPieces, pos.WPieces, endPositions, sq, Move.BLACK, Move.QUEEN, offset, moves);
        }

        // Black Bishop end

        // Black Rook move gen
        while (!pos.BRooks.isEmpty()){
            sq = pos.BRooks.popLsb();
            endPositions = RookMoves(sq, pos.Occupied, pos.BPieces);
            offset = drainBitboard(pos.BPieces, pos.WPieces, endPositions, sq, Move.BLACK, Move.QUEEN, offset, moves);
        }
        // Black Rook end

        // Black Queen move gen

        while (!pos.BQueens.isEmpty()){
            sq = pos.BQueens.popLsb();
            endPositions = QueenMoves(sq, pos.Occupied, pos.BPieces);
            offset = drainBitboard(pos.BPieces, pos.WPieces, endPositions, sq, Move.BLACK, Move.QUEEN, offset, moves);
        }

        // Black Queen end.

        // Black King move gen
        //TODO: add castles
        while (!pos.BKing.isEmpty()){
            int startSq = pos.BKing.popLsb();
            //get all end positions possible from startSq
            endPositions = Move.KING_LOOKUP[startSq];
            offset = drainBitboard(pos.BPieces, pos.WPieces, endPositions, startSq, Move.BLACK, Move.QUEEN, offset, moves);
        }
        return offset;
    }

    /**
     * Generates all moves for the White side
     * @param pos the current positions / gamestate
     * @param offset offset to start in move arr
     * @param moves all moves till full depth
     * @return new offset into moves arr
     */
    private static int generateMovesW (PositionEncoder.Position pos, int offset, int[] moves){
        // White Pawn move gen

        // single & double pushes can't take anything
        long singleP = (pos.WPawns.board << PositionEncoder.SIZE) & ~pos.Occupied;
        long doubleP = ((pos.WPawns.board & pos.WDoublePawnMove.board) << PositionEncoder.SIZE * 2)  & ~pos.Occupied;
        // check if field is occupied (by enemy) when trying to take
        long attackL = (pos.WPawns.board << PositionEncoder.SIZE - 1) & pos.BPieces;
        long attackR = (pos.WPawns.board << PositionEncoder.SIZE + 1) & pos.BPieces;

        // check if any attack fields land on the enPassant square
        long enPassantL = (pos.WPawns.board << PositionEncoder.SIZE - 1)
                & (pos.enPassant);
        long enPassantR = (pos.WPawns.board << PositionEncoder.SIZE + 1)
                & (pos.enPassant);

        // White Pawn end

        // get actual moves
        offset = formatPawnMoves(singleP, PositionEncoder.SIZE, offset, Move.FLAG_GENERIC, Move.WHITE, moves);
        offset = formatPawnMoves(doubleP, PositionEncoder.SIZE * 2, offset, Move.FLAG_GENERIC, Move.WHITE, moves);
        offset = formatPawnMoves(attackL, PositionEncoder.SIZE - 1, offset, Move.FLAG_CAPTURE, Move.WHITE, moves);
        offset = formatPawnMoves(attackR, PositionEncoder.SIZE + 1, offset, Move.FLAG_CAPTURE, Move.WHITE, moves);
        offset = formatPawnMoves(enPassantL, PositionEncoder.SIZE - 1, offset, Move.FLAG_EN_PASSANT, Move.WHITE, moves);
        offset = formatPawnMoves(enPassantR, PositionEncoder.SIZE + 1, offset, Move.FLAG_EN_PASSANT, Move.WHITE, moves);

        // White Knight move gen
        while (!pos.WKnights.isEmpty()){
            int startSq = pos.WKnights.popLsb();
            //get all end positions possible from startSq
            long endPositions = Move.KNIGHT_LOOKUP[startSq];
            offset = drainBitboard(pos.WPieces, pos.BPieces, endPositions, startSq, Move.WHITE, Move.KNIGHT, offset, moves);
        }

        // White Knight end

        // White-Bishops
        int sq;
        long endPositions;
        while (!pos.WBishops.isEmpty()){
            sq = pos.WBishops.popLsb();
            endPositions = BishopMoves(sq, pos.Occupied, pos.WPieces);
            offset = drainBitboard(pos.WPieces, pos.BPieces, endPositions, sq, Move.WHITE, Move.BISHOP, offset, moves);
        }

        // White Bishop end

        // White Rook move gen
        while (!pos.WRooks.isEmpty()){
            sq = pos.WRooks.popLsb();
            endPositions = RookMoves(sq, pos.Occupied, pos.WPieces);
            offset = drainBitboard(pos.WPieces, pos.BPieces, endPositions, sq, Move.WHITE, Move.ROOK, offset, moves);
        }
        // White Rook end

        // White Queen move gen

        while (!pos.WQueens.isEmpty()){
            sq = pos.WQueens.popLsb();
            endPositions = QueenMoves(sq, pos.Occupied, pos.WPieces);
            offset = drainBitboard(pos.WPieces, pos.BPieces, endPositions, sq, Move.WHITE, Move.QUEEN, offset, moves);
        }

        // White Queen end.

        // White King move gen
        // TODO: add castles
        while (!pos.WKing.isEmpty()){
            int startSq = pos.WKing.popLsb();
            //get all end positions possible from startSq
            endPositions = Move.KING_LOOKUP[startSq];
            offset = drainBitboard(pos.WPieces, pos.BPieces, endPositions, startSq, Move.WHITE, Move.KING, offset, moves);
        }
        return offset;
    }


    /**
     * turns a bitboard of final positions to encoded moves
     * @param ownPieces pieces of the same color
     * @param enemyPieces pieces of the opposite color
     * @param endPositions bitboard of endpositions
     * @param startSq starting square
     * @param offset offset into the moves arr
     * @param moves array of moves till max MCTS depth
     * @param color 0 for white 1 for black
     * @param PieceType PieceType constants from Move class
     * @return new offset into moves
     */
    private static int drainBitboard(long ownPieces, long enemyPieces, long endPositions, int startSq, int color, int PieceType, int offset, int[] moves){
        // ignore moves ending on own pieces
        endPositions &= ~ownPieces;

        while (endPositions != 0){
            int square = Long.numberOfTrailingZeros(endPositions);
            endPositions &= endPositions - 1;
            // is there an enemy piece
            if((enemyPieces >>> square & 1L) == 1L){
                moves[offset] = Move.of(startSq, square, Move.FLAG_CAPTURE, color, PieceType);
            }else {
                moves[offset] = Move.of(startSq, square, Move.FLAG_GENERIC, color, PieceType);
            }
            offset++;
        }
        return offset;
    }

    /**
     * Returns all moves visitable by a queen on square
     * @param square position of the queen
     * @param occupied bitboard of occupied positions
     * @param ownPieces bitboard of pos occupied by own piecs
     * @return all moves
     */
    private static long QueenMoves(int square, long occupied, long ownPieces){
        long endAttackPos = 0L;

        endAttackPos |= RookMoves(square, occupied, ownPieces); // Up, Down, Left, Right
        endAttackPos |= RookMoves(square, occupied, ownPieces); // Right-Up, Left-Up, Right-Down, Left-Down

        return endAttackPos;
    }

    /**
     * Returns all moves visitable by a rook on square
     * @param square position of the rook
     * @param occupied bitboard of occupied positions
     * @param ownPieces bitboard of pos occupied by own piecs
     * @return all moves
     */
    private static long RookMoves(int square, long occupied, long ownPieces){
        long endAttackPos = 0L;

        endAttackPos |= slideAttacks(square, PositionEncoder.SIZE, occupied, ownPieces); // Up
        endAttackPos |= slideAttacks(square, -PositionEncoder.SIZE, occupied, ownPieces); // Down
        endAttackPos |= slideAttacks(square, -1, occupied, ownPieces); // Left
        endAttackPos |= slideAttacks(square, 1, occupied, ownPieces); // Right
        return endAttackPos;
    }

    /**
     * Returns all moves visitable by a bishop on square
     * @param square position of the bishop
     * @param occupied bitboard of occupied positions
     * @param ownPieces bitboard of pos occupied by own pieces
     * @return all moves
     */
    private static long BishopMoves(int square, long occupied, long ownPieces){
        long endAttackPos = 0L;

        endAttackPos |= slideAttacks(square, PositionEncoder.SIZE+1, occupied, ownPieces); // Right-Up
        endAttackPos |= slideAttacks(square, PositionEncoder.SIZE-1, occupied, ownPieces); // Left-Up
        endAttackPos |= slideAttacks(square, -PositionEncoder.SIZE+1, occupied, ownPieces); // Right-Down
        endAttackPos |= slideAttacks(square, -PositionEncoder.SIZE-1, occupied, ownPieces); // Left-Down

        return endAttackPos;
    }

    /**
     * Slide attacks like rook, bishop and queen
     * in one direction till block
     * @param square start-square
     * @param delta offset to move (dir)
     * @param occupied bitboard of occupied positions
     * @param ownPieces bitboard of pos occupied by own pieces
     * @return all moves in that direction
     */
    private static long slideAttacks(int square, int delta, long occupied, long ownPieces){
        long attack = 0L;

        long b = 1L << square;
        // go until fall from board or collision with piece
        while(true){
            b <<= delta;
            if(b == 0L) // fall of board
                break;

            attack |= b; // set attack

            if((b & occupied) != 0L) { // any piece collision

                break;
            }
        }
        //remove any colls with own pieces
        attack &= ~ownPieces;

        return attack;
    }



    /**
     * receive a long bitboard with endpositions
     * fill the moves arr with the recalced moves
     *
     * @param bitBoard end positions
     * @param moveOffset offset used to get to the end position
     * @param offset offset in the moves array
     * @param moves arrays of moves to max search
     * @param color Color 0 for white 1 for black
     * @return new offset
     */
    private static int formatPawnMoves(long bitBoard, int moveOffset, int offset, int flags, int color, int[] moves){
        while(bitBoard != 0){
            int sq = Long.numberOfTrailingZeros(bitBoard);
            bitBoard &= bitBoard - 1;  // clears lowest set bit
            moves[offset] = Move.of(sq - moveOffset, sq, color, Move.PAWN, flags);
            offset ++;
        }
        return offset;
    }
}
