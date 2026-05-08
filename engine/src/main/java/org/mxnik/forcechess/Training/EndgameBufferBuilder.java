package org.mxnik.forcechess.Training;

import net.chesstango.gardel.fen.FEN;
import net.chesstango.piazzolla.syzygy.Syzygy;
import net.chesstango.piazzolla.syzygy.SyzygyPosition;
import org.mxnik.forcechess.General.ConsoleBar;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Moves.GameState;
import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.MoveGen;
import org.mxnik.forcechess.Pos.PolicyIndex;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.io.IOException;
import java.util.Random;

import static java.lang.Math.abs;
import static java.lang.Math.clamp;
import static org.mxnik.forcechess.Pos.Piece.*;
import static org.mxnik.forcechess.Pos.PositionEncoder.SIZE;
import static org.mxnik.forcechess.Pos.PositionUtils.place;
import static org.mxnik.forcechess.Pos.PositionUtils.toFen;
import static org.mxnik.forcechess.bot.ChessBot.MAX_MOVES_IN_POS;

public class EndgameBufferBuilder {
    // seed for reproducible outcomes
    private final static int SEED = 42;
    private final static float WIN_WDL_BASE = 9F;
    private final static float WIN_CLAMP = 10 - WIN_WDL_BASE;
    private final static float CURSED_WIN_WDL_BASE = 1F;
    private final static float CURSED_WIN_CLAMP = 1F;
    private final static float BLESSED_LOSS_WDL_BASE = -1F;
    private final static float BLESSED_LOSS_CLAMP = 1F;
    private final static float LOSS_WDL_BASE = -9F;
    private final static float LOSS_CLAMP = 10 - WIN_WDL_BASE;
    private final static float BEST_MOVE_VALUE = 11F;
    private final static float SOFTMAX_TEMP = 0.8F;

    private final Random pieceCGen;
    private final int[] tempBuffer = new int[MAX_MOVES_IN_POS];
    private final float[] policyV = new float[Move.MOVE_POSSIBILITIES];

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
            int pType;
            int sq;

            pos.updateHelper();
            pType = pieceCGen.nextInt(PAWN, KING);
            sq = pieceCGen.nextInt(1, 64);

            place(pos, whiteToMove, pType, sq);

            // loop reasons:
            // side not to move (black) can't be in check
            // pawns on the first or last rank
            // overlapping with other pieces
            if(((pos.Occupied >> sq) & 0x1) == 1L) {
                while ((pos.checkChess(!whiteToMove) || pType == PAWN && (sq / SIZE == 0 || sq / SIZE == 7))) {
                    pos.clearOnBoard(pType, sq);
                    pos.updateHelper();
                    sq = (sq + 1) % 64;    // place 1 to the right (all spaces are tested within 64 times);
                    if(((pos.Occupied >> sq) & 0x1) == 1L)
                        continue;
                    place(pos, whiteToMove, pType, sq);
                }
            }



        }
    }

    /**
     * generates a legal position as a fenString with a fixed number of pieces including both kings
     */
     public String generateLegalFen(int pieceC){
         var pos = generateLegalPosition(pieceC);
         return toFen(pos);
    }

    /**
     * generates a legal position with a fixed number of pieces including both kings
     */
    public PositionEncoder.Position generateLegalPosition(int pieceC){
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
     *
     * Builds a SampleBuffer from games loaded via the Syzygy Library
     *
     * @param moveCount end length of the SampleBuffer
     * @param pieceC amount of pieces
     * @param playBest play the best move (after syzygy) every turn (stricter less diverse) <p>
     *                 or play the best move calculated via the formular (more diverse depending on params - maybe to little differentiation)
     */
    public void buildBufferOnEndgames(int moveCount, int pieceC, boolean playBest, String file) {
        System.out.println("\tBuilding Buffer on Endgames");
        System.out.printf("\tBuilding set with %d moves\n", moveCount);
        System.out.println("-".repeat(ConsoleBar.WIDTH + 2));
        // open file
        try(Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")){
            SampleBuffer buffer = new SampleBuffer(moveCount, file, false);
            int engineMove = 0;
            int moveCounter = 0;
            var pos = generateLegalPosition(pieceC);
            boolean firstPos = true;
        while (moveCounter < moveCount) {
            if (pos.getState(pos.whiteToMove) != GameState.Continue){
                pos = generateLegalPosition(pieceC); // generate new position after mate or stalemate
                firstPos = true;
            }
            String fen = toFen(pos);
            DiversePair<Integer, int[]> res = getEndgamePos(syzygy, fen);
            int best = res.first();
            int[] results = res.second();

            if (best != Syzygy.TB_RESULT_FAILED && results[0] != Syzygy.TB_RESULT_FAILED) {
                int bestWdl = Syzygy.TB_GET_WDL(best);      // check for wins
                if(firstPos && bestWdl < 3){// stalemate or worse
                    pos = generateLegalPosition(pieceC); // generate new position
                    continue;
                }
                firstPos = false;
                moveCounter++;

                // -2 to center stalemate at 0; div by 2 to get win = 1 cursed win 0.5 stalemate. 0; loss -1; cursed loss -0.5;
                float z = ((float) (bestWdl - 2) / 2);

                int bestDtzStart = Syzygy.TB_GET_DTZ(best);
                int bestFromSq = Syzygy.TB_GET_FROM(best);
                int bestToSq = Syzygy.TB_GET_TO(best);
                int bestPromotes = Syzygy.TB_GET_PROMOTES(best);


//                System.out.println("position: " + fen);
//                System.out.printf("Best move: %d -> %d | WDL: %d | DTZ: %d%n", fromSq, toSq, bestWdl, bestDtzStart);
//                System.out.println("-----------------");
                int bestMove = 0;
                float bestScore = Float.NEGATIVE_INFINITY;
                for (int r : results) {
                    if (r == Syzygy.TB_RESULT_FAILED) break;
                    int moveWdl = Syzygy.TB_GET_WDL(r);
                    int moveDtz = Syzygy.TB_GET_DTZ(r);
                    int moveFrom = Syzygy.TB_GET_FROM(r);
                    int moveTo = Syzygy.TB_GET_TO(r);
                    int movePromotes = Syzygy.TB_GET_PROMOTES(r);


                    float score = computeScore(moveWdl, moveDtz, bestDtzStart);
                    engineMove = Move.of(moveFrom, moveTo, Move.toFlags(pos, moveTo, movePromotes));
                    policyV[PolicyIndex.toPolicyIndex(engineMove)] = score;

                    if(score > bestScore){
                        bestScore = score;
                        bestMove = engineMove;
                    }
                }


                int bestPossMove = Move.of(bestFromSq, bestToSq, Move.toFlags(pos, bestToSq, bestPromotes));
                policyV[PolicyIndex.toPolicyIndex(bestPossMove)] = BEST_MOVE_VALUE;
                // soften slightly with lower temperature
                buffer.addSample(PositionEncoder.encodeFlat(pos), softMax(policyV, SOFTMAX_TEMP), z);
                // play actual best Move (syzygy) or the one found via own scoring
                pos.makeMove(playBest ? bestPossMove : bestMove);
                ConsoleBar.render((double) moveCounter /moveCount);
            }else {
                pos = generateLegalPosition(pieceC); // generate new position if position can no longer be found in table
                firstPos = true;
                //System.out.println("failed movegen or syzygy pull");
            }
        }
            buffer.writeSamples();
        }catch (IOException e){
            System.err.println("Error when querying for position (IOException)");
            e.printStackTrace();
        }
        System.out.printf("Finished set %d/%d\n", moveCount, moveCount);
    }

    /**
     * turns float[] into softmaxxed version of self.
     * modifies memory in place. No new float[] is allocated
     */
    private float[] softMax(float[] targets, float temp){
        float[] values = new float[targets.length];

        float max = targets[0] * temp;
        for (float val : targets)
            if (val * temp > max)
                max = val * temp;

        float sum = 0F;
        for (int i = 0; i < values.length; i++) {
            values[i] = (float) Math.exp(targets[i] * temp - max);          // classical SOFTMAX (projecting onto e^x)
            sum  += values[i];
        }

        for (int i = 0; i < values.length; i++)
            values[i] /= sum;

        return values;
    }

    /**
     * compute the score for a given wdl and dtz
     * mixture of relative DTZ betterment and general wdl score
     */
    private static float computeScore(int wdl, int dtzCurrent, int dtzStart) {
        double wdlBase;
        double dtzProgress;

        // DTZ progress: how much better is this move relative to where we started
        // Guard against dtzStart == 0 (already at zeroing move)
        double rawProgress = (dtzStart > 0) ? (1.0 - (double) dtzCurrent / dtzStart) : 0.0;

        switch (wdl) {
            case Syzygy.TB_WIN -> {          // 4 - clean win, full range
                wdlBase = WIN_WDL_BASE;
                dtzProgress = clamp(rawProgress, -WIN_CLAMP, WIN_CLAMP);
            }
            case Syzygy.TB_CURSED_WIN -> {   // 3 - won but 50-move rule, capped so never reaches 1.0
                wdlBase = CURSED_WIN_WDL_BASE;
                dtzProgress = clamp(rawProgress, -CURSED_WIN_CLAMP, CURSED_WIN_CLAMP);
            }
            case Syzygy.TB_DRAW -> {         // 2
                wdlBase = 0.0;
                dtzProgress = 0.0;
            }
            case Syzygy.TB_BLESSED_LOSS -> { // 1 - lost but 50-move rule saves it, mirror of cursed win
                wdlBase = BLESSED_LOSS_WDL_BASE;
                dtzProgress = clamp(rawProgress, -BLESSED_LOSS_CLAMP, BLESSED_LOSS_CLAMP);
            }
            case Syzygy.TB_LOSS -> {         // 0 - clean loss, full negative range
                wdlBase = LOSS_WDL_BASE;
                dtzProgress = clamp(rawProgress, -LOSS_CLAMP, LOSS_CLAMP);
            }
            default -> {
                wdlBase = 0.0;
                dtzProgress = 0.0;
            }
        }

        return (float) (wdlBase + dtzProgress);
    }

    public static void main(String[] args) {
        EndgameBufferBuilder eg = new EndgameBufferBuilder(SEED);
        eg.buildBufferOnEndgames(100000, 4, true, "endGame_5_Pieces");
    }

}
