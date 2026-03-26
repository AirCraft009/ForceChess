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
        if (whiteToMove){
            return generateMovesW(position, offset, moves);
        }


        return offset;
    }

    /**
     * generates all moves for the white side
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
        offset = formatMoves(singleP, PositionEncoder.SIZE, offset, Move.FLAG_GENERIC, moves);
        offset = formatMoves(doubleP, PositionEncoder.SIZE * 2, offset, Move.FLAG_GENERIC, moves);
        offset = formatMoves(attackL, PositionEncoder.SIZE - 1, offset, Move.FLAG_CAPTURE, moves);
        offset = formatMoves(attackR, PositionEncoder.SIZE + 1, offset, Move.FLAG_CAPTURE, moves);
        offset = formatMoves(enPassantL, PositionEncoder.SIZE - 1, offset, Move.FLAG_EN_PASSANT, moves);
        offset = formatMoves(enPassantR, PositionEncoder.SIZE + 1, offset, Move.FLAG_EN_PASSANT, moves);

        // White Knight move gen

        while (!pos.WKnights.isEmpty()){
            int startSq = pos.WKnights.popLsb();
            //get all end positions possible from startSq
            long endPositions = Move.KNIGHT_LOOKUP[startSq];
            // ignore moves ending on own pieces
            endPositions &= ~pos.WPieces;

            while (endPositions != 0){
                int square = Long.numberOfTrailingZeros(endPositions);
                endPositions &= endPositions - 1;
                // is there an enemy piece
                if((pos.BPieces >>> square & 1L) == 1L){
                    moves[offset] = Move.of(startSq, square, Move.FLAG_CAPTURE);
                }else {
                    moves[offset] = Move.of(startSq, square, Move.FLAG_GENERIC);
                }
                offset++;
            }
        }



        return offset;
    }



    /**
     * receive a long bitboard with endpositions
     * fill the moves arr with the recalced moves
     *
     * @param bitBoard end positions
     * @param moveOffset offset used to get to the end position
     * @param offset offset in the moves array
     * @param moves arrays of moves to max search
     * @return new offset
     */
    private static int formatMoves(long bitBoard, int moveOffset, int offset, int flags,  int[] moves){
        while(bitBoard != 0){
            int sq = Long.numberOfTrailingZeros(bitBoard);
            bitBoard &= bitBoard - 1;  // clears lowest set bit
            moves[offset] = Move.of(sq - moveOffset, sq, flags);
            offset ++;
        }
        return offset;
    }
}
