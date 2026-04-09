package org.mxnik.forcechess.engine.bot;

import org.mxnik.forcechess.Util.Bitboard;
import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.engine.MCTS.MctsTree;
import org.mxnik.forcechess.engine.Pos.Move;
import org.mxnik.forcechess.engine.Pos.MoveGen;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;
import org.mxnik.forcechess.user.ChessLogic.GameState;

import java.util.Arrays;

/**
 * Final class connecting all classes from Pos to the NN
 */
public class ChessBot {
    public static final int MAX_SEARCH_DEPTH = 64;
    public static final int MAX_MOVES_IN_POS = 256;

    public final PositionEncoder.Position pos;                                  // state
    public final MctsTree tree;                                                 // eval the states and chose with PUCT
    public final int[] moves = new int[MAX_MOVES_IN_POS * MAX_SEARCH_DEPTH];    // pre-allocated move array to max search depth to avoid rapid allocs. and deallocs. in train-loop
    private final int[] movePtrStack = new int[MAX_SEARCH_DEPTH + 1];           // contains the ptr to one above the last entry. Has a 0 entry on 0 idx for simplicity purp. (example: segment 1: 0 - 12; movePtrStack[0] = 13
    public final int[] undoInfoStack = new int[MAX_SEARCH_DEPTH];               // all undoInformation in a stack so it can be accessed easily; access[cDepth - 1]
    public final float[] moveDist = new float[Move.MOVE_POSSIBILITIES];         // will hold the distributions for all the most likely moves;
    public final Evaluator evaluator;
    private int depth = 1;                                                      // depth = 1 da root immer existiert


    public ChessBot(Evaluator evaluator){
        this.evaluator = evaluator;
        pos = PositionEncoder.Position.StartingPosition();
        tree = new MctsTree();
    }

    /**
     * simulate one full round;
     * <p>
     * walk to a leafNode,
     * expand it,
     * backpropagate the values up,
     * unmake all moves
     */
    public void simulate(){
        int leafN = walkNode();
        backProp(leafN, tree.w[leafN]);
        unmakeAll();
    }

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
     * gets all possible moves and then adds them to the current-node as children
     * <p>
     * sets the policy and move of the child-node
     */
    private void expand(int node, int depth, float[] policyV) {
        // depth - 1 to get the last offset
        var out = MoveGen.generateMovesAndResult(pos, movePtrStack[depth-1], pos.whiteToMove, moves);
        movePtrStack[depth] = out.first();

        if(out.second() != GameState.Continue){     // don't expand if the game has ended
            return;
        }

        // iterate over all moves in curr pos.
        for (int i = movePtrStack[depth - 1]; i < movePtrStack[depth]; i++) {
            int child = tree.addNewChild(node, moves[i]);
            tree.p[child] = policyV[i-movePtrStack[depth-1]];    // add a new node and set the policy vector
            tree.move[child] = moves[i];
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
    public float[] policyHead(){
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
    public void simGame(int n){
        int epoch = 0;
        while (true){
            int move = 0;
            try {
                move = bestMove(n);
            }catch (ArrayIndexOutOfBoundsException e){
                System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
                System.out.println(Bitboard.lsb(pos.WKing));
                System.out.println(Bitboard.lsb(pos.BKing));

                var o = MoveGen.generateMovesAndResult(pos, 0, pos.whiteToMove, new int[256]);
                System.out.println( o.second());
                System.out.println( o.first());
                break;
            }
            epoch++;
            pos.makeMove(move);
            System.out.println(pos.whiteMaterial);
            System.out.printf("move: %d -> %d\n", Move.from(move), Move.to(move));
            resetCore();
        }
    }


    public static void main(String[] args) {
        ChessBot bot = new ChessBot(new Evaluator.StubEvaluator());
    }
}
