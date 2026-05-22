package org.mxnik.forcechess.Training;

import org.mxnik.forcechess.Moves.GameState;
import org.mxnik.forcechess.Pos.MoveGen;
import org.mxnik.forcechess.Pos.Piece;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.util.Random;

import static java.lang.Math.abs;
import static org.mxnik.forcechess.Pos.Piece.KING;
import static org.mxnik.forcechess.Pos.Piece.PAWN;
import static org.mxnik.forcechess.Pos.PositionEncoder.SIZE;
import static org.mxnik.forcechess.Pos.PositionUtils.place;
import static org.mxnik.forcechess.Pos.PositionUtils.toFen;
import static org.mxnik.forcechess.bot.ChessBot.MAX_MOVES_IN_POS;

public class PosGen {
    private static Random pieceCGen = new Random();
    private static final int[] tempBuffer = new int[MAX_MOVES_IN_POS];

    public void setPieceCGen(int seed){
        pieceCGen = new Random(seed);
    }

    /**
     * generates a legal position with randomly placed pieces
     * while the position is guaranteed to be legal it's not guaranteed to be reachable in a real game
     * neither king can be in check at the position start, even if it is a legal pos
     * The position may be a stalemate
     *
     *
     *
     * @param wPieceCount amount of white pieces to place king excluded
     * @param bPieceCount amount of black pieces to place king excluded
     */
    static PositionEncoder.Position generateLegalPosition(int wPieceCount, int bPieceCount, boolean whiteToMove){

        var pos = PositionEncoder.Position.emptyPosition();

        // reserve the 0 spot for checking illegals
        int K_W_Pos = pieceCGen.nextInt(0, 64);
        int K_B_Pos = pieceCGen.nextInt(0, 64);

        // make sure it's not in the same position or adjacent

        while(abs(K_B_Pos / SIZE - K_W_Pos/SIZE) < 2 || abs(K_B_Pos % SIZE - K_W_Pos % SIZE) < 2){
            K_B_Pos = (K_W_Pos + pieceCGen.nextInt(0, 2000)) % 64;
        }

        place(pos, true, KING, K_W_Pos);
        place(pos, false, KING, K_B_Pos);

        // generate normal pieces
        placePieces((whiteToMove ? wPieceCount : bPieceCount), whiteToMove, pos);
        placePieces((!whiteToMove ? wPieceCount : bPieceCount), !whiteToMove, pos);

        pos.whiteToMove = whiteToMove;

        return pos;
    }

    private static void placePieces(int pCount, boolean whiteToMove, PositionEncoder.Position pos){
        for (int i = 0; i < pCount; i++) {
            // loop through until the position is valid
            int pType;
            int sq = 0;

            pos.updateHelper();
            pType = pieceCGen.nextInt(PAWN, KING);
            int piece = Piece.of(whiteToMove, pType);

            // loop reasons:
            // side not to move (black) can't be in check
            // pawns on the first or last rank
            // overlapping with other pieces
            while (true) {
                do sq = (sq + pieceCGen.nextInt(0, 10000)) % 64;
                while (((pos.Occupied >> sq) & 0x1) == 1L || pType == PAWN && (sq / SIZE == 0 || sq / SIZE == 7));
                byte preVP = pos.pieceMap[sq];
                place(pos, whiteToMove, pType, sq);

                if(pos.checkChess(!whiteToMove)){
                    pos.clearOnBoard(piece, sq);
                    pos.pieceMap[sq] = preVP;
                    pos.updateHelper();
                    continue;
                }
                break;
            }
        }
    }

    /**
     * generates a legal position as a fenString with a fixed number of pieces including both kings
     */
    public static String generateLegalFen(int pieceC){
        var pos = generateLegalPosition(pieceC);
        return toFen(pos);
    }

    /**
     * generates a legal position with a fixed number of pieces including both kings
     */
    public static PositionEncoder.Position generateLegalPosition(int pieceC){
        if(pieceC > 63){
            System.err.println("can't place more than 63 pieces");
            return null;
        }

        int whitePCount = pieceCGen.nextInt(0, pieceC - 1);    // dec for the kings
        // the rest are black pieces
        int blackPCount = (pieceC - 2) - whitePCount;      // remove two because of the kings
        return generateLegalPosition(whitePCount, blackPCount, pieceCGen.nextBoolean());
    }

    /**
     * might take long (simple loop over position till mate in one)
     */
    public static PositionEncoder.Position generateMateInOne(int pieceC){
        PositionEncoder.Position pos;
        int counter = 0;

        outerLoop:
        while (true) {
            pos = generateLegalPosition(pieceC);
            var moveC = MoveGen.generateMoves(pos, 0, pos.whiteToMove, tempBuffer);
            GameState s;
            counter ++;
            for (int i = 0; i < moveC; i++) {
                int unmake = pos.makeMove(tempBuffer[i]);
                s = pos.getState(pos.whiteToMove);
                pos.unmakeMove(unmake);
                if(s == GameState.CheckMate)
                    break outerLoop;
            }
        }
        return pos;
    }
}
