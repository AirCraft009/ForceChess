package org.mxnik.forcechess.Pos;

import org.mxnik.forcechess.Bitboard;

public class Check {

    public static boolean WhiteHasMoves(PositionEncoder.Position pos) {

        long endPositions = getEndPositionsW(pos);
        if (endPositions != 0)
            return true;



        // Knights
        long knights = pos.WKnights;
        while (!Bitboard.isEmpty(knights)) {
             int sq = Bitboard.lsb(knights);
             knights = Bitboard.popLsb(knights);
             if(!Bitboard.isEmpty(Move.KNIGHT_LOOKUP[sq] & ~pos.BPieces))
                 return true;
        }

        // Bishops
        long bishops = pos.WBishops;
        while (!Bitboard.isEmpty(bishops)) {
            int sq = Bitboard.lsb(bishops);
            bishops = Bitboard.popLsb(bishops);
            if(!Bitboard.isEmpty(MoveGen.bishopMoves(sq, pos.Occupied, pos.WPieces)))
                return true;
        }

        // Rooks
        long rooks = pos.WRooks;
        while (!Bitboard.isEmpty(rooks)) {
            int sq = Bitboard.lsb(rooks );
            rooks = Bitboard.popLsb(rooks);
            if(!Bitboard.isEmpty(MoveGen.rookMoves(sq, pos.Occupied, pos.WPieces)))
                return true;
        }

        // Queens
        long queens = pos.WQueens;
        while (!Bitboard.isEmpty(queens)) {
            int sq = Bitboard.lsb(queens);
            queens = Bitboard.popLsb(queens);
            if(!Bitboard.isEmpty(MoveGen.queenMoves(sq, pos.Occupied, pos.WPieces)))
                return true;
        }

        // Kings
        long king = pos.WKing;
        while (!Bitboard.isEmpty(king)) {
            int sq = Bitboard.lsb(king);
            king = Bitboard.popLsb(king);
            if(!Bitboard.isEmpty(Move.KING_LOOKUP[sq] & ~pos.BPieces))
                return true;
        }

        return false;
    }

    private static long getEndPositionsW(PositionEncoder.Position pos) {
        long endPositions = 0;


        // Pawn pushes — can't land on any occupied square
        long singleP = (pos.WPawns << PositionEncoder.SIZE) & ~pos.Occupied;
        // log. and with single push because every double push has to also have a single push avail. if not then pawn + firstRank = blocked
        long doubleP = (((pos.WPawns & pos.WDoublePawnMove) << PositionEncoder.SIZE * 2) & ~pos.Occupied) & singleP << PositionEncoder.SIZE;


        // Pawn attacks — must land on enemy square
        long attackL = (pos.WPawns << PositionEncoder.SIZE - 1) & pos.BPieces;
        long attackR = (pos.WPawns << PositionEncoder.SIZE + 1) & pos.BPieces;

        // En passant attacks
        long enPassant =  1L << pos.enPassantSquare;
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

        endPositions |= singleP | doubleP | attackPromotionL | attackPromotionR | promotion | attackL | attackR | enPassantL | enPassantR;
        return endPositions;
    }

    public static boolean BlackHasMoves(PositionEncoder.Position pos) {

        long endPositions = getEndPositionsB(pos);
        if (endPositions != 0)
            return true;


        // Knights
        long knights = pos.BKnights;
        while (!Bitboard.isEmpty(knights)) {
            int sq = Bitboard.lsb(knights);
            knights = Bitboard.popLsb(knights);
            if(!Bitboard.isEmpty(Move.KNIGHT_LOOKUP[sq] & ~pos.BPieces))
                return true;
        }

        // Bishops
        long bishops = pos.BBishops;
        while (!Bitboard.isEmpty(bishops)) {
            int sq = Bitboard.lsb(bishops);
            bishops = Bitboard.popLsb(bishops);
            if(!Bitboard.isEmpty(MoveGen.bishopMoves(sq, pos.Occupied, pos.BPieces)))
                return true;
        }

        // Rooks
        long rooks = pos.BRooks;
        while (!Bitboard.isEmpty(rooks)) {
            int sq = Bitboard.lsb(rooks );
            rooks = Bitboard.popLsb(rooks);
            if(!Bitboard.isEmpty(MoveGen.rookMoves(sq, pos.Occupied, pos.BPieces)))
                return true;
        }

        // Queens
        long queens = pos.BQueens;
        while (!Bitboard.isEmpty(queens)) {
            int sq = Bitboard.lsb(queens);
            queens = Bitboard.popLsb(queens);
            if(!Bitboard.isEmpty(MoveGen.queenMoves(sq, pos.Occupied, pos.BPieces)))
                return true;
        }

        // Kings
        long king = pos.BKing;
        while (!Bitboard.isEmpty(king)) {
            int sq = Bitboard.lsb(king);
            king = Bitboard.popLsb(king);
            if(!Bitboard.isEmpty(Move.KING_LOOKUP[sq] & ~pos.BPieces))
                return true;
        }

        return false;
    }

    private static long getEndPositionsB(PositionEncoder.Position pos) {
        long endPositions = 0;


        // Pawn pushes — can't land on any occupied square
        long singleP = (pos.BPawns << PositionEncoder.SIZE) & ~pos.Occupied;
        // log. and with single push because every double push has to also have a single push avail. if not then pawn + firstRank = blocked
        long doubleP = (((pos.BPawns & pos.BDoublePawnMove) << PositionEncoder.SIZE * 2) & ~pos.Occupied) & singleP << PositionEncoder.SIZE;


        // Pawn attacks — must land on enemy square
        long attackL = (pos.BPawns << PositionEncoder.SIZE - 1) & pos.BPieces;
        long attackR = (pos.BPawns << PositionEncoder.SIZE + 1) & pos.BPieces;

        // En passant attacks
        long enPassant =  1L << pos.enPassantSquare;
        long enPassantL = (pos.BPawns << PositionEncoder.SIZE - 1) & enPassant;
        long enPassantR = (pos.BPawns << PositionEncoder.SIZE + 1) & enPassant;

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

        endPositions |= singleP | doubleP | attackPromotionL | attackPromotionR | promotion | attackL | attackR | enPassantL | enPassantR;
        return endPositions;
    }
}
