package org.mxnik.forcechess.Training;

import net.chesstango.gardel.fen.FEN;
import net.chesstango.piazzolla.syzygy.Syzygy;
import net.chesstango.piazzolla.syzygy.SyzygyPosition;

import java.io.IOException;

public class EndgameTraining {


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

    public static void getEndgamePos(Syzygy syzygy) {
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

    public void trainOnEndgames() {
        try(Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")){
            getEndgamePos(syzygy);
        }catch (IOException e){
            System.err.println("Error when querying for position (IOException)");
            e.printStackTrace();
        }
    }


}
