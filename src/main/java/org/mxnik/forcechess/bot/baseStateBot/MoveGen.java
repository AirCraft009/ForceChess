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
    private static int generateMovesW (PositionEncoder.Position position, int offset, int[] moves){
        // White Pawn move gen
        long singleP = (position.WPawns.board << PositionEncoder.SIZE) & ~position.Occupied.board;
        long doubleP = ((position.WPawns.board & position.WDoublePawnMove.board) << PositionEncoder.SIZE * 2)  & ~position.Occupied.board;
        // check if field is occupied when trying to take
        long attackL = (position.WPawns.board << PositionEncoder.SIZE - 1) & position.Occupied.board;
        long attackR = (position.WPawns.board << PositionEncoder.SIZE + 1) & position.Occupied.board;

        // check if any attack fields land on the enPassant square
        long enPassantL = (position.WPawns.board << PositionEncoder.SIZE - 1)
                & (position.enPassant.board);
        long enPassantR = (position.WPawns.board << PositionEncoder.SIZE + 1)
                & (position.enPassant.board);

        // get actual moves
        offset = formatMoves(singleP, PositionEncoder.SIZE, offset, Move.FLAG_NONE, moves);
        offset = formatMoves(doubleP, PositionEncoder.SIZE * 2, offset, Move.FLAG_NONE, moves);
        offset = formatMoves(attackL, PositionEncoder.SIZE - 1, offset, Move.FLAG_CAPTURE, moves);
        offset = formatMoves(attackR, PositionEncoder.SIZE + 1, offset, Move.FLAG_CAPTURE, moves);
        offset = formatMoves(enPassantL, PositionEncoder.SIZE - 1, offset, Move.FLAG_EN_PASSANT, moves);
        offset = formatMoves(enPassantR, PositionEncoder.SIZE + 1, offset, Move.FLAG_EN_PASSANT, moves);

        // White Knights


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
