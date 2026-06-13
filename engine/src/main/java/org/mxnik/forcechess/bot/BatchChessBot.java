package org.mxnik.forcechess.bot;

import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.General.FlatArray;
import org.mxnik.forcechess.Moves.GameState;
import org.mxnik.forcechess.Pos.*;
import org.mxnik.forcechess.Training.EndgameBufferBuilder;
import org.mxnik.forcechess.network.AlphaNet;
import org.mxnik.forcechess.network.NetworkConfig;
import org.nd4j.linalg.api.buffer.DataType;

import java.io.IOException;
import java.util.Random;

import static org.mxnik.forcechess.MCTS.MctsTree.ROOT;

/**
 * Extension of the ChessBot that Batches moves to improve efficiency for GPU and CPU network evals
 */
public class BatchChessBot extends ChessBot{
    public static final int BATCH_SIZE = 64;
    public static final float VIRTUAL_LOSS = 1F;

    // there to remove virtualLoss and virtualVisitCount
    private final int[][] batchedMoves = new int[BATCH_SIZE][MAX_MOVES_IN_POS];                     // keeps next moves for each collected node in order
    private final DiversePair<Integer, GameState>[] endStates = new DiversePair[BATCH_SIZE];        // keeps endStates (number of moves in given pos & GameState) for all connected nodes
    private final int[] virtuallyAffectedNodes = new int[BATCH_SIZE];                               // all leaf-nodes affected by virtualLoss
    private final FlatArray batchedInputs;                                                          // array to keep the inputTensors of all collected nodes
    private final BatchEvaluator evaluator;                                                         // the batchevaluator overriding the normal evaluator in ChessBot
    private final float temp;

    /**
     * Initializes a BatChessBot with the starting pos
     * @param evaluator what evaluation should be used
     * @param playDepth how many MCTS iters a Bot should do
     */
    public BatchChessBot(BatchEvaluator evaluator, int playDepth, float temp) {
        super(null, playDepth);
        this.evaluator = evaluator;
        batchedInputs  = new FlatArray(BATCH_SIZE, PositionEncoder.TENSOR_SIZE);
        this.temp = temp;
    }

    /**
     * Initializes a BatChessBot with a given position
     * @param evaluator what evaluation should be used
     * @param playDepth how many MCTS iters a Bot should do
     * @param fen fen-string of the given position
     */
    public BatchChessBot(BatchEvaluator evaluator, String fen, int playDepth, float temp) {
        super(null, fen, playDepth);
        this.evaluator = evaluator;
        batchedInputs  = new FlatArray(BATCH_SIZE, PositionEncoder.TENSOR_SIZE);
        this.temp = temp;
    }


    /**
     * Initializes a BatChessBot with a given position
     * @param evaluator what evaluation should be used
     * @param playDepth how many MCTS iters a Bot should do
     * @param pos Built position object
     */
    public BatchChessBot(BatchEvaluator evaluator, PositionEncoder.Position pos, int playDepth) {
        super(null, playDepth);
        this.pos = pos;
        System.out.println(PositionUtils.toFen(pos));
        this.evaluator = evaluator;
        batchedInputs  = new FlatArray(BATCH_SIZE, PositionEncoder.TENSOR_SIZE);
        temp = 1;
    }

    @Override
    public Evaluator getEvaluator(){
        return evaluator;
    }

    /**
     * simulate one full round;
     * <p>
     * walk to a leafNode,
     * expand it,
     * backpropagate the values up,
     * unmake all moves
     */
    @Override
    public void simulate(){
        walkNode();
        expandAndPropagate();
        unmakeAll();
    }


    /**
     * returns the move with the highest visit count after n (to the closest batch) moves
     */
    @Override
    public int bestMoveUCB(int n){
        // an entire batch is evaluated at once
        for (int i = 0; i < n; i+=BATCH_SIZE) {
            simulate();
        }
        return tree.move[tree.highestVisitNode(0)];
    }

    /**
     * returns the move with the highest score after n simulations (to nearest BATCH_SIZE)
     */
    @Override
    public int bestMove(int n){
        expandRoot();
        for (int i = 0; i < n; i+=BATCH_SIZE) {
            simulate();
        }
        outputMoveDist(true);
        float[] moveDist = tree.moveDistChild();


        return tree.move[weightedRandomIndex(moveDist, temp)];
    }

    public int weightedRandomIndex(float[] values, float bias) {
        if(bias == 1){
            return tree.highestVisitNode(ROOT);
        }

        Random random = new Random();

        float total = 0f;

        for (float value : values) {
            total += (1f - bias) + bias * value;
        }

        float r = random.nextFloat() * total;

        float cumulative = 0f;

        for (int i = 0; i < values.length; i++) {
            cumulative += (1f - bias) + bias * values[i];

            if (r < cumulative) {
                return i;
            }
        }

        return values.length - 1;
    }


    /**
     * walks down from root till a leaf node is hit.
     * then applies virtual loss.
     * then goes back up till a batch of leafNodes is found
     * expands them all at once
     */
    protected void walkNode(){
        int nodeCount = 0;
        int node = 0;
        depth = 0;

        while (nodeCount < BATCH_SIZE) {
            if(depth == MAX_SEARCH_DEPTH){
                tree.globalVisits++;
                tree.n[ROOT]++;
                updateVirtual(node);
                batchedMoves[nodeCount] = new int[0];           // empty array
                virtuallyAffectedNodes[nodeCount] = node;
                endStates[nodeCount] = new DiversePair<>(0,GameState.Continue);
                PositionEncoder.encode(nodeCount * PositionEncoder.TENSOR_SIZE, pos, batchedInputs.arr);

                nodeCount++;
                unmakeAll();
                System.out.println("Hit depth");
//                resetCore();

                depth = 0;
                node = ROOT;
                continue;
            }


            if (tree.firstChild[node] == 0) {
                tree.globalVisits++;
                tree.n[ROOT]++;
                updateVirtual(node);
                virtuallyAffectedNodes[nodeCount] = node;
                endStates[nodeCount] = MoveGen.generateMovesAndResult(pos, pos.whiteToMove, batchedMoves[nodeCount]);


                /*
                the last move was played and now the position is to be rated from the side that played it

                for example white.
                pos.whiteToMove was flipped and now the position is encoded from the view of black
                now all evals are the inverse of what they should be

                That's why I flip the value

                OMG kwasdhjgiopjkasdjvbijklwasjedgjkvbjasdklj jlk LET'S GOOOO

                 */

                pos.whiteToMove = !pos.whiteToMove;     // flip color so encoding happens from the correct perspective
                PositionEncoder.encode(nodeCount * PositionEncoder.TENSOR_SIZE, pos, batchedInputs.arr);            // save the position for later eval
                pos.whiteToMove = !pos.whiteToMove;     // flip back for future moves


                // reset the tree so another batch run can be started
                nodeCount++;
                unmakeAll();

                depth = 0;
                node = ROOT;
                continue;
            }
            int bestC = tree.findBestChild(node);
            undoInfoStack[depth] = pos.makeMove(tree.move[bestC]);
            depth++;
            node = bestC;
        }
    }


    /**
     * evaluates and expands all collected nodes
     * removes the virtual-loss from each node
     * propagates w up the tree
     */
    protected void expandAndPropagate(){

        Evaluator.Result[] results = evaluator.evaluateBatch(batchedInputs.arr);

        // all nodes collected in batch
        for (int i = 0; i < virtuallyAffectedNodes.length; i++) {
            int node = virtuallyAffectedNodes[i];


            switch (endStates[i].second()){
                case Continue -> {
                    backProp(node, results[i].value());
                }
                case CheckMate -> {
                    // lost the game from playing persp.
                    System.out.println("hit mate");
                    backProp(node, 1.2F);
                }
                case StaleMate, FiftyMove -> {
                    backProp(node, -0.2F);
                }
            }



            if(tree.firstChild[node] == 0){

                // don't add after game end
                if(endStates[i].second() != GameState.Continue)
                    continue;

                normalizeDist(results[i].policyV(), batchedMoves[i], endStates[i].first());

                // expand out all moves and set the policy
                for (int j = 0; j < endStates[i].first(); j++) {
                    int child = tree.addNewChild(node, batchedMoves[i][j]);
                    tree.p[child] = results[i].policyV()[PolicyIndex.toPolicyIndex(batchedMoves[i][j])];
                    //tree.p[child] = results[i].policyV()[PolicyIndex.toPolicyIndex(batchedMoves[i][j])];
                }
            }
        }
    }


    /**
     * Backpropagate virtual loss from a node
     */
    private void updateVirtual(int node){
        while (node != 0) {
            // don't add to n it was alr incremented during the batching process to make the node look worse

            tree.w[node] -= VIRTUAL_LOSS;
            tree.n[node]++;
            node = tree.parentIdx[node];
        }
    }


    /**
     * Backpropagate a given value from a node
     * @param val the position rating (z)
     */
    @Override
    protected void backProp(int node, float val){
        while (node != 0) {
            // don't add to n it was alr incremented during the batching process to make the node look worse

            tree.w[node] += val         // add the value
            + VIRTUAL_LOSS;             // remove the Virtual-loss from each node
            val = -val;                 // flip val because one move is done by black the other by white (alternating)
            node = tree.parentIdx[node];
        }
    }

    @Override
    public void close() throws IOException {
        evaluator.close();
    }



    public static void main(String[] args) throws IOException {
        EndgameBufferBuilder eg = new EndgameBufferBuilder(110);
        var net = NetworkConfig.buildNet();
        net.getConfiguration().setInferenceWorkspaceMode(WorkspaceMode.ENABLED);
        net.convertDataType(DataType.FLOAT16);
        BatchChessBot bc = new BatchChessBot(new AlphaNet(net), 400,1);
        bc.selfPlayGame(200);
    }

}
