package org.mxnik.forcechess.Training;

import net.chesstango.gardel.fen.FEN;
import net.chesstango.piazzolla.syzygy.Syzygy;
import net.chesstango.piazzolla.syzygy.SyzygyPosition;
import org.mxnik.forcechess.Pos.Piece;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.io.IOException;
import java.util.Random;

import static java.lang.Math.abs;
import static org.mxnik.forcechess.Pos.PositionEncoder.SIZE;
import static org.mxnik.forcechess.Pos.PositionUtils.place;
import static org.mxnik.forcechess.Pos.PositionUtils.toFen;

public class EndgameTraining {
    // seed for reproducible outcomes
    private final static int SEED = 42;
    private Random pieceCGen = new Random(42);

    public EndgameTraining(int seed){
        pieceCGen = new Random(seed);
    }


    // THIS METHOD WAS TAKEN FROM GOOGLE AI
    /**
     * Converts WDL percentages to a centipawn evaluation score.
     *
     * @param winProb  The win probability for White (range: 0.0 to 1.0)
     * @param drawProb The draw probability (range: 0.0 to 1.0)
     * @param lossProb The loss probability for White (range: 0.0 to 1.0)
     * @return The evaluation in centipawns (positive for White, negative for Black)
     */
    static int convertWdlToCentipawns(double winProb, double drawProb, double lossProb) {
        // Step 1: Compute expected score (S) ranging from 0.0 to 1.0
        double expectedScore = winProb + 0.5 * drawProb;

        // Clip bounds to prevent Infinity or NaN errors in logarithmic calculations
        expectedScore = Math.clamp(expectedScore, 0.0001, 0.9999);

        // Step 2: Use the standard logistic/sigmoid mapping
        // In Lc0/Stockfish, 100 cp is calibrated to roughly a 50% win chance.
        // We use the modern sigmoid inverse formula.
        double scoreTransform = expectedScore / (1.0 - expectedScore);

        // This is a fitted scaling factor that anchors a 50% win probability to ~100 cp.
        double scalingFactor = 290.68;
        double centipawns = scalingFactor * Math.log(scoreTransform);

        return (int) Math.round(centipawns);
    }
    // AI END

    public void getEndgamePos(Syzygy syzygy) {
            //  WDL probe: just "is this a win?" (fast, no move)
            FEN fen = FEN.of("7k/8/7K/7Q/8/8/8/8 w - - 0 1");
            SyzygyPosition pos = SyzygyPosition.from(fen);


            //  Root probe: best move + WDL for ALL legal moves
            int[] results = new int[Syzygy.TB_MAX_MOVES];  // TB_MAX_MOVES = 193
            int best = syzygy.tb_probe_root(pos, results);

            if (best != Syzygy.TB_RESULT_FAILED) {
                int wdlScore = Syzygy.TB_GET_WDL(best);    // 4=WIN, 3=CURSED_WIN, 2=DRAW, 1=BLESSED_LOSS, 0=LOSS
                int dtz = Syzygy.TB_GET_DTZ(best);    // distance-to-zeroing (proxy for DTM)
                int fromSq = Syzygy.TB_GET_FROM(best);   // square index 0-63
                int toSq = Syzygy.TB_GET_TO(best);
                int promotes = Syzygy.TB_GET_PROMOTES(best); // 0=none,1=Q,2=R,3=B,4=N

                // Iterate all legal moves with their WDL
                for (int r : results) {
                    if (r == Syzygy.TB_RESULT_FAILED) break;  // sentinel, array is null-terminated
                    int moveWdl = Syzygy.TB_GET_WDL(r);
                    int moveFrom = Syzygy.TB_GET_FROM(r);
                    int moveTo = Syzygy.TB_GET_TO(r);
                    // use these to build your training samples
                }
            }
    }

    /**
     * generates a legal position with randomly placed pieces
     * @param wPieceCount amount of white pieces to place king included
     * @param bPieceCount amount of black pieces to place king included
     */
    PositionEncoder.Position generateLegalPosition(int wPieceCount, int bPieceCount, boolean whiteToMove){
        if(wPieceCount < 1 || bPieceCount < 1){
            throw new IllegalArgumentException("can't generate a position with < 1 pieces on either side");
        }

        var pos = PositionEncoder.Position.emptyPosition();

        int K_W_Pos = pieceCGen.nextInt(0, 64);
        int K_B_Pos = pieceCGen.nextInt(0, 64);

        // make sure it's not in the same position or adjacent

        if(abs(K_B_Pos / SIZE - K_W_Pos/SIZE) < 2 || abs(K_B_Pos % SIZE - K_W_Pos % SIZE) < 2){
            K_B_Pos = (K_W_Pos + 2) % 64;
        }

        place(pos, true, Piece.KING, K_W_Pos);
        place(pos, false, Piece.KING, K_B_Pos);


        return pos;
    }

    /**
     * generates legal 5 piece (all-inclusive) positions and formats it into a fen String
     */
     String generateLegalFen(){
         int whitePCount = pieceCGen.nextInt(0, 4);
         int blackPCount = pieceCGen.nextInt(0, 4 - whitePCount);

         return toFen(generateLegalPosition(whitePCount, blackPCount, pieceCGen.nextBoolean()));
    }

    public void trainOnEndgames() {
        try(Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")){
            getEndgamePos(syzygy);
        }catch (IOException e){
            System.err.println("Error when querying for position (IOException)");
            e.printStackTrace();
        }
    }


}
