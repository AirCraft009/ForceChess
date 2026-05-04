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

public class EndgameTraining {
    // seed for reproducible outcomes
    private final static int SEED = 42;
    private Random pieceCGen;

    public EndgameTraining(int seed){
        pieceCGen = new Random(seed);
    }

    public  EndgameTraining(){
        pieceCGen = new Random();
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
         int whitePCount = pieceCGen.nextInt(0, pieceC - 1);    // dec for the kings
         // the rest are black pieces
         int blackPCount = (pieceC - 2) - whitePCount;      // remove two because of the kings



         return toFen(generateLegalPosition(whitePCount, blackPCount, pieceCGen.nextBoolean()));
    }

    public void trainOnEndgames() {
        try(Syzygy syzygy = Syzygy.open("boardsNBots/bots/Syzygy_Bases/Syzygy")){
            DiversePair<Integer, int[]> res = getEndgamePos(syzygy, generateLegalFen(5));

        }catch (IOException e){
            System.err.println("Error when querying for position (IOException)");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        EndgameTraining eg = new EndgameTraining();
    }

}
