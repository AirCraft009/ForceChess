package org.mxnik.forcechess.Training;

import net.chesstango.gardel.fen.FEN;
import net.chesstango.piazzolla.syzygy.Syzygy;
import net.chesstango.piazzolla.syzygy.SyzygyPosition;
import org.mxnik.forcechess.DiversePair;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.io.IOException;
import java.util.Random;

import static java.lang.Math.abs;
import static org.mxnik.forcechess.Pos.Piece.*;
import static org.mxnik.forcechess.Pos.PositionEncoder.SIZE;
import static org.mxnik.forcechess.Pos.PositionUtils.place;
import static org.mxnik.forcechess.Pos.PositionUtils.toFen;

public class EndgameBufferBuilder {
    // seed for reproducible outcomes
    private final static int SEED = 42;
    private Random pieceCGen;

    public EndgameBufferBuilder(int seed){
        pieceCGen = new Random(seed);
    }

    public EndgameBufferBuilder(){
        pieceCGen = new Random();
    }



    public DiversePair<Integer, int[]> getEndgamePos(Syzygy syzygy, String fenStr) {
            //  WDL probe: just "is this a win?" (fast, no move)
            FEN fen = FEN.of(fenStr);
            SyzygyPosition pos = SyzygyPosition.from(fen);


            //  Root probe: best move + WDL for ALL legal moves
            int[] results = new int[Syzygy.TB_MAX_MOVES];  // TB_MAX_MOVES = 193
            int best = syzygy.tb_probe_root(pos, results);
            return new DiversePair<>(best, results);
    }



    /**
     * generates a legal position with randomly placed pieces
     * while the position is guaranteed to be legal it's not guaranteed to be reachable in a real game
     * neither king can be in check at the position start, even if it is a legal pos
     * lastly no piece will be generated on A1/0 as it's used for checking
     * The position may be a stalemate
     *
     *
     *
     * @param wPieceCount amount of white pieces to place king excluded
     * @param bPieceCount amount of black pieces to place king excluded
     */
    PositionEncoder.Position generateLegalPosition(int wPieceCount, int bPieceCount, boolean whiteToMove){

        var pos = PositionEncoder.Position.emptyPosition();

        // reserve the 0 spot for checking illegals
        int K_W_Pos = pieceCGen.nextInt(1, 64);
        int K_B_Pos = pieceCGen.nextInt(1, 64);

        // make sure it's not in the same position or adjacent

        if(abs(K_B_Pos / SIZE - K_W_Pos/SIZE) < 2 || abs(K_B_Pos % SIZE - K_W_Pos % SIZE) < 2){
            K_B_Pos = (K_W_Pos + 2) % 64;
        }

        place(pos, true, KING, K_W_Pos);
        place(pos, false, KING, K_B_Pos);

        // generate normal pieces
        placePieces((whiteToMove ? wPieceCount : bPieceCount), whiteToMove, pos);
        placePieces((!whiteToMove ? wPieceCount : bPieceCount), !whiteToMove, pos);

        pos.whiteToMove = whiteToMove;

        return pos;
    }

    private void placePieces(int pCount, boolean whiteToMove, PositionEncoder.Position pos){
        for (int i = 0; i < pCount; i++) {
            // loop through until the position is valid
            int pType = EMPTY_PIECE;
            int sq = 0;
            do {
                pos.clearOnBoard(pType, sq);
                pos.updateHelper();
                pType = pieceCGen.nextInt(PAWN, KING);
                sq = pieceCGen.nextInt(1, 64);
                while (((pos.Occupied >> sq) & 0x1) == 1L){// while loop to ensure it isn't moved to the second king on accident
                    sq = (sq + 16) % 64;    // place up two squares;
                }

                place(pos, whiteToMove, pType, sq);

                // loop reasons:
                // side not to move (black) can't be in check
                // pawns on the first or last rank
            }while (pos.checkChess(!whiteToMove)
                    || (pType == PAWN && (sq / SIZE == 0 || sq / SIZE == 7)));
        }
    }

    /**
     * generates a legal position with a fixed number of pieces including both kings
     */
     String generateLegalFen(int pieceC){
         if(pieceC > 63){
             System.err.println("can't place more than 63 pieces");
             return null;
         }

         int whitePCount = pieceCGen.nextInt(0, pieceC - 1);    // dec for the kings
         // the rest are black pieces
         int blackPCount = (pieceC - 2) - whitePCount;      // remove two because of the kings

         return toFen(generateLegalPosition(whitePCount, blackPCount, pieceCGen.nextBoolean()));
    }

    public void trainOnEndgames() {
        try(Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")){
            String fen =  generateLegalFen(5);
            DiversePair<Integer, int[]> res = getEndgamePos(syzygy, fen);
            int best = res.first();
            int[] results = res.second();

            if (best != Syzygy.TB_RESULT_FAILED) {
                int bestWdl = Syzygy.TB_GET_WDL(best);
                int dtzStart = Syzygy.TB_GET_DTZ(best);
                int fromSq = Syzygy.TB_GET_FROM(best);
                int toSq = Syzygy.TB_GET_TO(best);
                int promotes = Syzygy.TB_GET_PROMOTES(best);

                System.out.println("position: " + fen);
                System.out.printf("Best move: %d -> %d | WDL: %d | DTZ: %d%n", fromSq, toSq, bestWdl, dtzStart);
                System.out.println("-----------------");

                for (int r : results) {
                    if (r == Syzygy.TB_RESULT_FAILED) break;

                    int moveWdl  = Syzygy.TB_GET_WDL(r);
                    int moveDtz  = Syzygy.TB_GET_DTZ(r);
                    int moveFrom = Syzygy.TB_GET_FROM(r);
                    int moveTo   = Syzygy.TB_GET_TO(r);

                    double score = computeScore(moveWdl, moveDtz, dtzStart);

                    System.out.printf("move: %d -> %d | WDL: %d | DTZ: %d | score: %.4f%n",
                            moveFrom, moveTo, moveWdl, moveDtz, score);
                }
            }
        }catch (IOException e){
            System.err.println("Error when querying for position (IOException)");
            e.printStackTrace();
        }
    }

    /**
     * compute the score for a given wdl and dtz
     * mixture of relative DTZ betterment and general wdl score
     */
    private static double computeScore(int wdl, int dtzCurrent, int dtzStart) {
        double wdlBase;
        double dtzProgress;

        // DTZ progress: how much better is this move relative to where we started
        // Guard against dtzStart == 0 (already at zeroing move)
        double rawProgress = (dtzStart > 0) ? (1.0 - (double) dtzCurrent / dtzStart) : 0.0;

        switch (wdl) {
            case Syzygy.TB_WIN -> {          // 4 - clean win, full range
                wdlBase = 0.5;
                dtzProgress = Math.clamp(rawProgress, -0.5, 0.5);
            }
            case Syzygy.TB_CURSED_WIN -> {   // 3 - won but 50-move rule, capped so never reaches 1.0
                wdlBase = 0.1;
                dtzProgress = Math.clamp(rawProgress, -0.1, 0.1);
            }
            case Syzygy.TB_DRAW -> {         // 2
                wdlBase = 0.0;
                dtzProgress = 0.0;
            }
            case Syzygy.TB_BLESSED_LOSS -> { // 1 - lost but 50-move rule saves it, mirror of cursed win
                wdlBase = -0.1;
                dtzProgress = Math.clamp(rawProgress, -0.1, 0.1);
            }
            case Syzygy.TB_LOSS -> {         // 0 - clean loss, full negative range
                wdlBase = -0.5;
                dtzProgress = Math.clamp(rawProgress, -0.5, 0.5);
            }
            default -> {
                wdlBase = 0.0;
                dtzProgress = 0.0;
            }
        }

        return wdlBase + dtzProgress;
    }

    public static void main(String[] args) {
        EndgameBufferBuilder eg = new EndgameBufferBuilder();
        eg.trainOnEndgames();
    }

}
