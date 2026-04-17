package org.mxnik.forcechess.engine.bot;

import org.mxnik.forcechess.Util.FlatArray;
import org.mxnik.forcechess.engine.MCTS.MctsTree;
import org.mxnik.forcechess.engine.Pos.Move;
import org.mxnik.forcechess.engine.Pos.MoveGen;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;
import org.mxnik.forcechess.engine.network.AlphaNet;
import org.mxnik.forcechess.engine.network.NetworkConfig;
import org.mxnik.forcechess.engine.network.SampleBuffer;
import org.mxnik.forcechess.user.ChessLogic.GameState;

/**
 * Final class connecting all classes from Pos to the NN
 */
public class ChessBot {
    public static final int MAX_SEARCH_DEPTH = 64;
    public static final int MAX_MOVES_IN_POS = 256;
    public static final int BATCH_SIZE = 32;
    public static final float VIRTUAL_LOSS = 1F;


    private final PositionEncoder.Position pos;                                  // state
    private final MctsTree tree;                                                 // eval the states and chose with PUCT
    private final int[] moves = new int[MAX_MOVES_IN_POS * MAX_SEARCH_DEPTH];    // pre-allocated move array to max search depth to avoid rapid allocs. and deallocs. in train-loop
    private final int[] movePtrStack = new int[MAX_SEARCH_DEPTH + 1];            // contains the ptr to one above the last entry. Has a 0 entry on 0 idx for simplicity purp. (example: segment 1: 0 - 12; movePtrStack[0] = 13
    private final int[] undoInfoStack = new int[MAX_SEARCH_DEPTH];               // all undoInformation in a stack so it can be accessed easily; access[cDepth - 1]
    private final float[] moveDist = new float[Move.MOVE_POSSIBILITIES];         // will hold the distributions for all the most likely moves;
    // there to remove virtualLoss and virtualVisitCount
    private final int[] virtuallyAffectedNodes = new int[BATCH_SIZE * MAX_SEARCH_DEPTH];          // there can be at most this amount of nodes effected by virtualLoss
    private final FlatArray batchedInputs = new FlatArray(BATCH_SIZE, PositionEncoder.TENSOR_SIZE);

    private final Evaluator evaluator;
    private int depth = 1;                                                      // depth = 1 da root immer existiert


    public ChessBot(Evaluator evaluator){
        this.evaluator = evaluator;
        pos = PositionEncoder.Position.StartingPosition();
        tree = new MctsTree();
    }

    /**
     * simulate one full round(batched);
     * <p>
     * walk to Batchsize leafNodes,
     * expand them,
     * backpropagate the values up,
     * unmake all moves
     */
    public void simulate(){
        int leafN = walkNode();
        backProp(leafN, tree.w[leafN]);
        unmakeAll();
    }

    /**
     * simulate one full round;
     * <p>
     * walk to a leafNode,
     * expand it,
     * backpropagate the values up,
     * unmake all moves
     */
    public void simulateBatched(){
        walkNodesBatched();
        expandBatched();
        backPropBatched();
        unmakeAll();
    }


    /**
     * unmake all moves made up until now back to S0
     */
    private void unmakeAll(){
        depth -= 2;        // remove leftover depth and set it to point at value not above
        for (int i = depth; i  >= 0 ; i--) {
            pos.unmakeMove(undoInfoStack[i]);
        }
    }

    /**
     * walks down from root till a leaf node is hit.
     * then expands the leafnode
     * @return the leaf node
     */
    private int walkNode(){
        int node = 0;
        depth = 1;

        while (true) {
            if(depth == MAX_SEARCH_DEPTH){
                Evaluator.Result v = evaluator.evaluate(pos);
                tree.n[node]++;
                tree.w[node] += v.value();
                return node;
            }

            if (tree.firstChild[node] == 0) {
                Evaluator.Result v = evaluator.evaluate(pos);
                expand(node, depth, v.policyV());               // add all moves to the end
                tree.n[node]++;
                tree.w[node] += v.value();
                return node;
            }
            int bestC = tree.findBestChild(node);
            undoInfoStack[depth-1] = pos.makeMove(tree.move[bestC]);
            depth++;
            node = bestC;
        }
    }

    /**
     * walks down from root till a leaf node is hit.
     * then applies virtual loss.
     * then goes back up till a batch of leafNodes is found
     * expands them all at once
     */
    private void walkNodesBatched(){
        int nodeCount = 0;
        int node = 0;
        depth = 1;

        while (nodeCount < BATCH_SIZE) {
            if(depth == MAX_SEARCH_DEPTH){
                tree.globalVisits++;
                tree.n[node]++;
                tree.w[node] -= VIRTUAL_LOSS;
                backProp(node, -VIRTUAL_LOSS);
                virtuallyAffectedNodes[nodeCount] = node;
                PositionEncoder.encode(nodeCount * PositionEncoder.TENSOR_SIZE, pos, batchedInputs.arr);

                nodeCount++;
                unmakeAll();
                resetCore();
                continue;
            }

            if (tree.firstChild[node] == 0) {
                tree.globalVisits++;
                tree.n[node]++;
                tree.w[node] -= VIRTUAL_LOSS;           // apply virtual loss
                backProp(node, -VIRTUAL_LOSS);          // backpropagate virtual-loss back up
                virtuallyAffectedNodes[nodeCount] = node;
                PositionEncoder.encode(nodeCount * PositionEncoder.TENSOR_SIZE, pos, batchedInputs.arr);            // save the position for later eval

                // reset the tree so another batch run can be started
                nodeCount++;
                unmakeAll();
                resetCore();
                continue;
            }
            int bestC = tree.findBestChild(node);
            undoInfoStack[depth-1] = pos.makeMove(tree.move[bestC]);
            depth++;
            node = bestC;
        }
    }

    private void expandBatched(){
        for (int i = 0; i < virtuallyAffectedNodes.length; i++) {
            int node = virtuallyAffectedNodes[i];
            if(tree.firstChild[node] != 0)
                continue;


        }
    }

    /**
     * gets all possible moves and then adds them to the current-node as children
     * <p>
     * sets the policy and move of the child-node
     */
    private void expand(int node, int depth, float[] policyV) {
        // depth - 1 to get the last offset
        var out = MoveGen.generateMovesAndResult(pos, movePtrStack[depth-1], pos.whiteToMove, moves);
        movePtrStack[depth] = out.first();

        // iterate over all moves in curr pos.
        for (int i = movePtrStack[depth - 1]; i < movePtrStack[depth]; i++) {
            int child = tree.addNewChild(node, moves[i]);
            tree.p[child] = policyV[moves[i]];    // add a new node and set the policy vector
        }
    }

    private void backProp(int node, float val){
        tree.globalVisits++;
        tree.n[0]++;
        while (node != 0){
            tree.n[node]++;
            tree.w[node] += val;
            val = -val;                 // flip val because one move is done by black the other by white (alternating)
            node = tree.parentIdx[node];
        }
    }

    private void backPropBatched(){
        for (int virtuallyAffectedNode : virtuallyAffectedNodes) {
            int node = virtuallyAffectedNode;
            float val = tree.w[node];
            while (node != 0) {
                tree.n[node]++;
                tree.w[node] += val;
                val = -val;                 // flip val because one move is done by black the other by white (alternating)
                node = tree.parentIdx[node];
            }
        }
    }

    /**
     * returns the move with the highest visit count after n moves
     */
    public int bestMove(int n){
        for (int i = 0; i < n; i++) {
            simulate();
        }
        return tree.move[tree.highestVisitNode(0)];
    }



    /**
     * simulates a whole iteration n times
     */
    public void run(int n){
        for (int i = 0; i < n; i++) {
            simulate();
        }
    }

    /**
     * returns the probability dist. over root children
     */
    public float[] moveDist(){
        int node = tree.firstChild[0];
        while (node != 0){
            moveDist[tree.move[node]] = (float) tree.n[node] / tree.globalVisits;
            node = tree.nextSibling[node];
        }
        return moveDist;
    }


    /**
     * resets values so that a new position can be sent to the tree
     */
    public void resetCore(){
        tree.reset();
        depth = 1;
    }


    /**
     * sims a game and outputs to the screen
     * every move will have n rounds in the tree
     */
    public int selfPlayGame(int n, int startoffset, int end, SampleBuffer buffer){

        float z = 0;
        float[] flat;
        int startPtr = buffer.getPtr();
        GameState g = pos.getState(pos.whiteToMove);

        System.out.println("startGame");
        int move;
        while (g == GameState.Continue && startoffset < end){

            flat = PositionEncoder.encodeFlat(pos);
            move = bestMove(n);
            buffer.addSample(flat, moveDist.clone(), z);
            pos.makeMove(move);
            System.out.printf("move: %d -> %d\n", Move.from(move), Move.to(move));
            resetCore();
            g = pos.getState(pos.whiteToMove);
            startoffset ++;
        }

        z = pos.whiteToMove && g == GameState.CheckMate ? 1 : -1;
        buffer.updateZ(startPtr, z);
        return startoffset;
    }


    public static void main(String[] args) {
        ChessBot bot = new ChessBot(new AlphaNet(NetworkConfig.buildNet()));
        SampleBuffer buffer = new SampleBuffer(1, "");
        bot.selfPlayGame(1, 0,1, buffer);
        System.out.println(buffer.sample());
    }
}
