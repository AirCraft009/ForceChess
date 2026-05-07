package org.mxnik.forcechess.Training;

import net.chesstango.gardel.fen.FEN;
import net.chesstango.piazzolla.syzygy.Syzygy;
import net.chesstango.piazzolla.syzygy.SyzygyPosition;
import org.mxnik.forcechess.Bitboard;
import org.mxnik.forcechess.DiversePair;
import org.mxnik.forcechess.GameState;
import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.MoveGen;
import org.mxnik.forcechess.Pos.PolicyIndex;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.io.IOException;
import java.util.Arrays;
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
    private final static float WIN_WDL_BASE = 5F;
    private final static float WIN_CLAMP = 10 - WIN_WDL_BASE;
    private final static float CURSED_WIN_WDL_BASE = 0.5F;
    private final static float CURSED_WIN_CLAMP = 0.5F;
    private final static float BLESSED_LOSS_WDL_BASE = -0.5F;
    private final static float BLESSED_LOSS_CLAMP = 0.5F;
    private final static float LOSS_WDL_BASE = -5F;
    private final static float LOSS_CLAMP = 10 - WIN_WDL_BASE;

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

    public void buildBufferOnEndgames(int moveCount, int pieceC, String file) {

        // open file
        try(Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")){
            SampleBuffer buffer = new SampleBuffer(moveCount, file, false);
            int engineMove = 0;
            int moveCounter = 0;
            var pos = generateLegalPosition(pieceC);
            boolean firstPos = true;
        while (moveCounter < moveCount) {

            if (MoveGen.generateMovesAndResult(pos, pos.whiteToMove, tempBuffer).second() != GameState.Continue){
                pos = generateLegalPosition(pieceC); // generate new position after mate or stalemate
                firstPos = true;
            }

            String fen = toFen(pos);
            DiversePair<Integer, int[]> res = getEndgamePos(syzygy, fen);
            int best = res.first();
            int[] results = res.second();

            if (best != Syzygy.TB_RESULT_FAILED) {
                int bestWdl = Syzygy.TB_GET_WDL(best);      // check for wins
                if(firstPos && bestWdl < 3){                           // stalemate or worse
                    pos = generateLegalPosition(pieceC); // generate new position
                    continue;
                }
                firstPos = false;
                moveCounter++;

                // -2 to center stalemate at 0; div by 2 to get win = 1 cursed win 0.5 stalem. 0; loss -1; cursed loss -0.5;
                float z = ((float) (bestWdl - 2) / 2);

                int dtzStart = Syzygy.TB_GET_DTZ(best);
                int fromSq = Syzygy.TB_GET_FROM(best);
                int toSq = Syzygy.TB_GET_TO(best);
                int bestPromote = Syzygy.TB_GET_PROMOTES(best);
                int syzygyBestM = Move.of(fromSq, toSq, Move.toFlags(pos, toSq, bestPromote));

//                System.out.println("position: " + fen);
//                System.out.printf("Best move: %d -> %d | WDL: %d | DTZ: %d%n", fromSq, toSq, bestWdl, dtzStart);
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


                    float score = computeScore(moveWdl, moveDtz, dtzStart);
                    engineMove = Move.of(moveFrom, moveTo, Move.toFlags(pos, moveTo, movePromotes));
                    if(syzygyBestM == engineMove){
                        score += WIN_WDL_BASE;
                        System.out.println("correct this happens once every run: " + score);
                    }
                    policyV[PolicyIndex.toPolicyIndex(engineMove)] = score + ((Move.of(fromSq, toSq, Move.toFlags(pos, toSq, bestPromote)) == engineMove)? WIN_WDL_BASE : 0);

                    if(score > bestScore){
                        System.out.println("new bestScore: " + score);
                        System.out.printf("new bestMove: %d -> %d\n", Move.from(engineMove), Move.to(engineMove));
                        bestScore = score;
                        bestMove = engineMove;
                    }
                }

                float[] softmaxxed = softMax(policyV, 2F);
                buffer.addSample(PositionEncoder.encodeFlat(pos), softmaxxed, z);
//               System.out.printf("bestM: %d -> %d\n", Move.from(bestMove), Move.to(bestMove));
                System.out.println("new Move");
                System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
                System.out.println(Move.from(bestMove));
                System.out.println(Move.to(bestMove));
                System.out.println("best Move");
                System.out.println(fromSq);
                System.out.println(toSq);

                pos.makeMove(bestMove);
            }else {
                pos = generateLegalPosition(pieceC); // generate new position if position can no longer be found in table
                firstPos = true;
            }
        }
            buffer.writeSamples();
        }catch (IOException e){
            System.err.println("Error when querying for position (IOException)");
            e.printStackTrace();
        }
    }

    /**
     * turns float[] into softmaxxed version of self.
     * modifies memory in place. No new float[] is allocated
     */
    private float[] softMax(float[] targets, float temp){
        float[] values = new float[targets.length];
        float max = targets[0];
        for (float val : targets)
            if (val * temp > max)
                max = val * temp;

        float sum = 0F;
        for (int i = 0; i < targets.length; i++) {
            values[i] = (float) Math.exp((targets[i] * temp) - max);          // classical SOFTMAX (projecting onto e^x)
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
        eg.buildBufferOnEndgames(10, 5, "endGame_5_Pieces");
    }

}
