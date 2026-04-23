package org.mxnik.forcechess.bot;

import org.mxnik.forcechess.DiversePair;
import org.mxnik.forcechess.FlatArray;
import org.mxnik.forcechess.GameState;
import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.MoveGen;
import org.mxnik.forcechess.Pos.PositionEncoder;

import static org.mxnik.forcechess.MCTS.MctsTree.ROOT;

public class BatchChessBot extends ChessBot{
    public static final int BATCH_SIZE = 32;
    public static final float VIRTUAL_LOSS = 1F;

    // there to remove virtualLoss and virtualVisitCount
    private final int[][] batchedMoves = new int[BATCH_SIZE][Move.MOVE_POSSIBILITIES];
    private final DiversePair<Integer, GameState>[] endStates = new DiversePair[BATCH_SIZE];
    private final int[] virtuallyAffectedNodes = new int[BATCH_SIZE];          // all leaf-nodes affected by virtualLoss
    private final FlatArray batchedInputs;
    private final BatchEvaluator evaluator;

    public BatchChessBot(BatchEvaluator evaluator, int playDepth) {
        super(null, playDepth);
        this.evaluator = evaluator;
        batchedInputs  = new FlatArray(BATCH_SIZE, PositionEncoder.TENSOR_SIZE);
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
    public int bestMove(int n){
        // an entire batch is evaluated at once
        for (int i = 0; i < n; i+=BATCH_SIZE) {
            simulate();
        }
        return tree.move[tree.highestVisitNode(0)];
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
                tree.n[node]++;
                updateVirtual(node);
                batchedMoves[nodeCount] = new int[0];           // empty array
                virtuallyAffectedNodes[nodeCount] = node;
                endStates[nodeCount] = new DiversePair<>(0,GameState.StaleMate);
                PositionEncoder.encode(nodeCount * PositionEncoder.TENSOR_SIZE, pos, batchedInputs.arr);

                nodeCount++;
                unmakeAll();
                resetCore();

                depth = 0;
                node = ROOT;
                continue;
            }

            if (tree.firstChild[node] == 0) {
                tree.globalVisits++;
                tree.n[node]++;
                updateVirtual(node);
                virtuallyAffectedNodes[nodeCount] = node;
                endStates[nodeCount] = MoveGen.generateMovesAndResult(pos, pos.whiteToMove, batchedMoves[nodeCount]);
                PositionEncoder.encode(nodeCount * PositionEncoder.TENSOR_SIZE, pos, batchedInputs.arr);            // save the position for later eval

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

            // set the value
            backProp(node, tree.w[node]);


            if(tree.firstChild[node] == 0){

                // don't add after game end
                if(endStates[i].second() != GameState.Continue)
                    continue;

                // expand out all moves and set the policy
                for (int j = 0; j < endStates[i].first(); j++) {
                    int child = tree.addNewChild(node, batchedMoves[i][j]);
                    tree.p[child] = results[i].policyV()[batchedMoves[i][j]];
                }
            }
        }
    }

    private void updateVirtual(int node){
        while (node != 0) {
            // don't add to n it was alr incremented during the batching process to make the node look worse

            tree.w[node] -= VIRTUAL_LOSS;
            node = tree.parentIdx[node];
        }
    }

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




}
