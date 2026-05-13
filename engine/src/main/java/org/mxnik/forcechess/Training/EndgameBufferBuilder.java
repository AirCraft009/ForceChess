package org.mxnik.forcechess.Training;

import net.chesstango.gardel.fen.FEN;
import net.chesstango.piazzolla.syzygy.Syzygy;
import net.chesstango.piazzolla.syzygy.SyzygyPosition;
import org.mxnik.forcechess.General.ConsoleBar;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Moves.GameState;
import org.mxnik.forcechess.Pos.*;
import org.mxnik.forcechess.bot.ChessBot;
import org.mxnik.forcechess.network.AlphaNet;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.Math.*;
import static org.mxnik.forcechess.Pos.Piece.*;
import static org.mxnik.forcechess.Pos.PositionEncoder.SIZE;
import static org.mxnik.forcechess.Pos.PositionUtils.*;
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
    private final static float BEST_MOVE_VALUE = Syzygy.TB_WIN + 1F;
    private final static float SOFTMAX_TEMP = 1.2F;

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

    private void placePieces(int pCount, boolean whiteToMove, PositionEncoder.Position pos){
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
     * might take long (simple loop over position till mate in one)
     */
    public PositionEncoder.Position generateMateInOne(int pieceC){
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

    /**
     *
     * Builds a SampleBuffer from games loaded via the Syzygy Library
     *
     * @param moveCount end length of the SampleBuffer
     * @param pieceC amount of pieces
     */
    public SampleBuffer buildBufferOnEndgames(int moveCount, int pieceC, boolean onlyWins, Function<Integer, PositionEncoder.Position> positionGenerator, String file) {
        System.out.println("\tBuilding Buffer on Endgames");
        System.out.printf("\tBuilding set with %d moves\n", moveCount);
        System.out.println("-".repeat(ConsoleBar.WIDTH + 2));
        // open file
        try(Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")){
            SampleBuffer buffer = new SampleBuffer(moveCount, file, false);
            int engineMove = 0;
            int moveCounter = 0;
            var pos = positionGenerator.apply(pieceC);
            boolean firstPos = true;
        while (moveCounter < moveCount) {
            if (pos.getState(pos.whiteToMove) != GameState.Continue){
                pos = positionGenerator.apply(pieceC); // generate new position after mate or stalemate
                firstPos = true;
            }
            String fen = toFen(pos);
            DiversePair<Integer, int[]> res = getEndgamePos(syzygy, fen);
            int best = res.first();
            int[] results = res.second();

            if (best != Syzygy.TB_RESULT_FAILED && results[0] != Syzygy.TB_RESULT_FAILED) {
                int bestWdl = Syzygy.TB_GET_WDL(best);                  // check for wins
                if(firstPos && (onlyWins &&  bestWdl != Syzygy.TB_WIN) || bestWdl == Syzygy.TB_DRAW){   // not a win
                    pos = positionGenerator.apply(pieceC);                // generate new position
                    continue;
                }

                firstPos = false;

                // -2 to center stalemate at 0; div by 2 to get win = 1 cursed win 0.5 stalemate. 0; loss -1; cursed loss -0.5;
                float z = ((float) (bestWdl - 2) / 2);

                int bestDtzStart = Syzygy.TB_GET_DTZ(best);
                int bestFromSq = Syzygy.TB_GET_FROM(best);
                int bestToSq = Syzygy.TB_GET_TO(best);
                int bestPromotes = Syzygy.TB_GET_PROMOTES(best);

                if(onlyWins && bestWdl != Syzygy.TB_WIN){       // skip moves of the other position because they just fill up buffer space
                    pos.makeMove(Move.of(bestFromSq, bestToSq, Move.toFlags(pos, bestToSq, bestPromotes)));
                    continue;
                }

                moveCounter++;


//                System.out.println("position: " + fen);
//                System.out.printf("Best move: %d -> %d | WDL: %d | DTZ: %d%n", fromSq, toSq, bestWdl, bestDtzStart);
//                System.out.println("-----------------");
                Arrays.fill(policyV, Float.NEGATIVE_INFINITY);
                for (int r : results) {
                    if (r == Syzygy.TB_RESULT_FAILED) break;
                    int moveWdl = Syzygy.TB_GET_WDL(r);
                    int moveFrom = Syzygy.TB_GET_FROM(r);
                    int moveTo = Syzygy.TB_GET_TO(r);
                    int movePromotes = Syzygy.TB_GET_PROMOTES(r);


                    float score = (moveWdl - Syzygy.TB_DRAW);
                    if (!pos.whiteToMove) {
                        moveFrom = moveFrom ^ 56;  // flip square vertically
                        moveTo   = moveTo   ^ 56;
                    }
                    engineMove = Move.of(moveFrom, moveTo, Move.toFlags(pos, moveTo, movePromotes));
                    policyV[PolicyIndex.toPolicyIndex(engineMove)] = score;
                }

                // flipped from and two in case of !whiteToMove
                int PCorrectedF = bestFromSq;
                int PCorrectedT = bestToSq;

                // flip because board is also flipped
                if (!pos.whiteToMove) {
                    PCorrectedF = bestFromSq ^ 56;  // flip square vertically
                    PCorrectedT   = bestToSq   ^ 56;
                }
                int bestPossMove = Move.of(bestFromSq, bestToSq, Move.toFlags(pos, bestToSq, bestPromotes));
                int bestFlippedMove = Move.of(PCorrectedF, PCorrectedT, Move.toFlags(pos, bestToSq, bestPromotes));
                policyV[PolicyIndex.toPolicyIndex(bestFlippedMove)] = BEST_MOVE_VALUE;
                // soften slightly with lower temperature
                buffer.addSample(PositionEncoder.encodeFlat(pos), softMax(policyV, SOFTMAX_TEMP), z);
                // play actual best Move (syzygy) or the one found via own scoring
                pos.makeMove(bestPossMove);
                ConsoleBar.render((double) moveCounter /moveCount, 2);
            }else {
                pos = positionGenerator.apply(pieceC); // generate new position if position can no longer be found in table
                firstPos = true;
                //System.out.println("failed movegen or syzygy pull");
            }
        }
        System.out.printf("\nFinished set %d/%d\n", moveCount, moveCount);
        return buffer;
        }catch (IOException e){
            System.err.println("Error when querying for position (IOException)");
            e.printStackTrace();
        }
        return null;
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
            values[i] = (float) exp(targets[i] * temp - max);          // classical SOFTMAX (projecting onto e^x)
            sum  += values[i];
        }

        for (int i = 0; i < values.length; i++)
            values[i] /= sum;

        return values;
    }


    public static void main(String[] args) throws IOException {
        EndgameBufferBuilder eg = new EndgameBufferBuilder();

        for (int i = 0; i < 6; i++) {
            var b =          eg.buildBufferOnEndgames(50000, 3, true, eg::generateLegalPosition, "BalancedBuffer"+i);
            b.combineBuffers(eg.buildBufferOnEndgames(50000, 4, true, eg::generateLegalPosition, ""));
            b.combineBuffers(eg.buildBufferOnEndgames(50000, 5, true, eg::generateLegalPosition, ""));

            b.writeSamples();
        }
//            var pos = fromFen("4k3/7Q/4K3/8/8/8/8/8 w - - 0 1");
//        SampleBuffer buffer = new SampleBuffer(1, "", false);
//
//        try (Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")) {
//            var t = eg.getEndgamePos(syzygy, "4k3/7Q/4K3/8/8/8/8/8 w - - 0 1");
//            int best = t.first();
//            int[] results = t.second();
//
//            if (best != Syzygy.TB_RESULT_FAILED) {
//                int bestWdl = Syzygy.TB_GET_WDL(best);
//
//                float z = ((float) (bestWdl - 2) / 2);
//
//
//                int engineMove;
//                int bestDtzStart = Syzygy.TB_GET_DTZ(best);
//                int bestFromSq = Syzygy.TB_GET_FROM(best);
//                int bestToSq = Syzygy.TB_GET_TO(best);
//                int bestPromotes = Syzygy.TB_GET_PROMOTES(best);
//
//
////                System.out.println("position: " + fen);
////                System.out.printf("Best move: %d -> %d | WDL: %d | DTZ: %d%n", fromSq, toSq, bestWdl, bestDtzStart);
////                System.out.println("-----------------");
//                Arrays.fill(eg.policyV, Float.NEGATIVE_INFINITY);
//                for (int r : results) {
//                    if (r == Syzygy.TB_RESULT_FAILED) break;
//                    int moveWdl = Syzygy.TB_GET_WDL(r);
//                    int moveFrom = Syzygy.TB_GET_FROM(r);
//                    int moveTo = Syzygy.TB_GET_TO(r);
//                    int movePromotes = Syzygy.TB_GET_PROMOTES(r);
//
//
//                    float score = (moveWdl - Syzygy.TB_DRAW);
//                    if (!pos.whiteToMove) {
//                        moveFrom = moveFrom ^ 56;  // flip square vertically
//                        moveTo = moveTo ^ 56;
//                    }
//                    engineMove = Move.of(moveFrom, moveTo, Move.toFlags(pos, moveTo, movePromotes));
//                    eg.policyV[PolicyIndex.toPolicyIndex(engineMove)] = score;
//                }
//
//
//                // flip because board is also flipped
//                if (!pos.whiteToMove) {
//                    bestFromSq = bestFromSq ^ 56;  // flip square vertically
//                    bestToSq = bestToSq ^ 56;
//                }
//                int bestPossMove = Move.of(bestFromSq, bestToSq, Move.toFlags(pos, bestToSq, bestPromotes));
//                eg.policyV[PolicyIndex.toPolicyIndex(bestPossMove)] = BEST_MOVE_VALUE;
//                // soften slightly with lower temperature
//                float[] vals = eg.softMax(eg.policyV, 1.2F);
//
//                float max = 0;
//                int maxIdx = -1;
//                int nonZero = 0;
//                for (int i = 0; i < vals.length; i++) {
//                    if (vals[i] > 0) {
//                        nonZero++;
//                        System.out.println("idx " + i + " = " + vals[i]);
//                        if (vals[i] > max) { max = vals[i]; maxIdx = i; }
//                    }
//                }
//
//                int m = MoveGen.generateMoves(pos, 0, true, eg.tempBuffer);
//
//                for (int i = 0; i < m; i++) {
//                    int move = eg.tempBuffer[i];
//                    if (PolicyIndex.toPolicyIndex(move) == maxIdx) {
//                        System.out.println("best move: " + Move.format(move));
//                    }
//                }
//
//                buffer.addSample(PositionEncoder.encodeFlat(pos), vals, z);
//            }
//        }
//
//        Train train = new Train("D400_10_RES_BLOCKS", true);
//        train.train(128, buffer, 60, 0);
//        System.out.println("Loss of op: " + train.getLoss());
//
//        ChessBot b = new ChessBot(train.getNetwork(), 1, "4k3/7Q/4K3/8/8/8/8/8 w - - 0 1");
//        System.out.println(Move.format(b.bestMove(300)));
    }
}