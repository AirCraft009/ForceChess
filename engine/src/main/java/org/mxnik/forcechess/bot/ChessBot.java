package org.mxnik.forcechess.bot;

import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.Bitboard;
import org.mxnik.forcechess.MCTS.MctsTree;
import org.mxnik.forcechess.MovePacket;
import org.mxnik.forcechess.Player;
import org.mxnik.forcechess.Pos.*;
import org.mxnik.forcechess.network.AlphaNet;
import org.mxnik.forcechess.network.SampleBuffer;
import org.mxnik.forcechess.GameState;

import java.io.File;
import java.io.IOException;

import static org.mxnik.forcechess.MCTS.MctsTree.ROOT;

/**
 * Final class connecting all classes from Pos to the NN
 */
public class ChessBot implements Player {
    public static final int MAX_SEARCH_DEPTH = 64;
    public static final int MAX_MOVES_IN_POS = 256;

    protected PositionEncoder.Position pos;                                        // state
    protected final MctsTree tree;                                                 // eval the states and chose with PUCT
    protected final int[] moves = new int[MAX_MOVES_IN_POS];                       // pre-allocated move array to max search depth to avoid rapid allocs. and deallocs. in train-loop
    protected final int[] undoInfoStack = new int[MAX_SEARCH_DEPTH];               // all undoInformation in a stack so it can be accessed easily; access[cDepth - 1]
    protected final float[] moveDist = new float[Move.MOVE_POSSIBILITIES];         // will hold the distributions for all the most likely moves;

    protected final Evaluator evaluator;
    protected int depth = 1;                                                      // depth = 1 da root immer existiert
    private int playDepth;

    public ChessBot(Evaluator evaluator, int playDepth){
        this.evaluator = evaluator;
        this.playDepth = playDepth;
        pos = PositionEncoder.Position.StartingPosition();
        tree = new MctsTree();
    }

    public Evaluator getEvaluator(){
        return evaluator;
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

    public void  setPos(PositionEncoder.Position p){
        this.pos = p;
    }


    /**
     * unmake all moves made up until now back to S0
     */
    public void unmakeAll(){
        depth --;        // remove leftover depth and set it to point at value not above
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
        depth = 0;

        while (true) {
            if(depth == MAX_SEARCH_DEPTH){
                Evaluator.Result v = evaluator.evaluate(pos);
                tree.n[node]++;
                tree.w[node] += v.value();
                return node;
            }

            if (tree.firstChild[node] == 0) {
                Evaluator.Result v = evaluator.evaluate(pos);
                expand(node, v.policyV());               // add all moves to the end
                tree.n[node]++;
                tree.w[node] += v.value();
                return node;
            }
            int bestC = tree.findBestChild(node);
            undoInfoStack[depth] = pos.makeMove(tree.move[bestC]);
            depth++;
            node = bestC;
        }
    }

    /**
     * gets all possible moves and then adds them to the current-node as children
     * <p>
     * sets the policy and move of the child-node
     */
    protected void expand(int node, float[] policyV) {
        // depth - 1 to get the last offset
        var out = MoveGen.generateMovesAndResult(pos, pos.whiteToMove, moves);

        if(out.second() != GameState.Continue)
            return;


        // iterate over all moves in curr pos.
        for (int i = 0; i < out.first(); i++) {
            int child = tree.addNewChild(node, moves[i]);
            tree.p[child] = policyV[moves[i]];    // add a new node and set the policy vector
        }
    }

    protected void backProp(int node, float val){
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
    public int bestMoveUCB(int n){
        for (int i = 0; i < n; i++) {
            simulate();
        }
        return tree.move[tree.highestVisitNode(ROOT)];
    }

    /**
     * returns the move with the highest visit count after n moves
     */
    public int bestMove(int n){
        for (int i = 0; i < n; i++) {
            simulate();
        }
        return tree.move[tree.highestScoreChild(ROOT)];
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
    public void resetCore() {
        tree.reset();
        depth = 0;
    }

    protected void expandRoot(){
        Evaluator.Result r = getEvaluator().evaluate(pos);
        tree.w[ROOT] = r.value();
        expand(ROOT, r.policyV());

        tree.addNoiseToRootChildren();
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
        while (g == GameState.Continue && startoffset < end){       // loop until Check/stalemate or full buffer

            flat = PositionEncoder.encodeFlat(pos);     // save pos before move happens
            expandRoot();
            move = bestMoveUCB(n);
            buffer.addSample(flat, moveDist.clone(), z); // record the moveDist. and z value

            pos.makeMove(move);
            System.out.printf("move: %d -> %d\n", Move.from(move), Move.to(move));
            //System.out.printf("move: %d -> %d\n", Move.from(move), Move.to(move));
            resetCore();
            g = pos.getState(pos.whiteToMove);
            startoffset ++;
        }

        z = pos.whiteToMove && g == GameState.CheckMate ? 1 : -1;
        buffer.updateZ(startPtr, z);
        return startoffset;
    }

    public void selfPlayGame(int n){

        GameState g = pos.getState(pos.whiteToMove);

        System.out.println("startGame");
        int move;
        while (g == GameState.Continue){
            expandRoot();
            move = bestMoveUCB(n);
            pos.makeMove(move);
            System.out.printf("move: %d -> %d\n", Move.from(move), Move.to(move));
            resetCore();
            g = pos.getState(pos.whiteToMove);
        }

    }


    public static void main(String[] args) throws IOException {
        BatchChessBot bot = new BatchChessBot( new AlphaNet(ModelSerializer.restoreComputationGraph(
                new File("boardsNBots/bots/networks/D300.zip"), true
        )), 400);
        bot.selfPlayGame(400);
//        BatchChessBot b = new BatchChessBot(new BatchEvaluator.StubEvaluator());
//        b.selfPlayGame(300);
    }

    @Override
    public MovePacket requestMove() {
        int rMove = bestMove(playDepth);
        expandRoot();
        System.out.printf("Black: %d -> %d\n", Move.from(rMove), Move.to(rMove));
        pos.makeMove(rMove);
        System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
        resetCore();
        return Move.toMovePacket(rMove);
    }

    @Override
    public void getMove(MovePacket movePacket) {
        int move = Move.MovePacketToMove(movePacket);
        expandRoot();
        System.out.printf("whiteM: %d -> %d\n", Move.from(move), Move.to(move));
        System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
        pos.makeMove(move);
        resetCore();
    }
}