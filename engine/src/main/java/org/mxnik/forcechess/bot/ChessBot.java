package org.mxnik.forcechess.bot;

import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.General.Bitboard;
import org.mxnik.forcechess.General.ConsoleBar;
import org.mxnik.forcechess.MCTS.MctsTree;
import org.mxnik.forcechess.Moves.MovePacket;
import org.mxnik.forcechess.GameControl.Player;
import org.mxnik.forcechess.Pos.*;
import org.mxnik.forcechess.network.AlphaNet;
import org.mxnik.forcechess.Training.SampleBuffer;
import org.mxnik.forcechess.Moves.GameState;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;
import java.util.function.BiConsumer;

import static java.lang.Math.abs;
import static org.mxnik.forcechess.MCTS.MctsTree.ROOT;
import static org.mxnik.forcechess.Pos.PositionUtils.toFieldName;

/**
 * ChessBot combines an evaluator with and MCTS tree to improve playing beyond greedy sampling.
 */
public class ChessBot implements Player {
    public static final int MAX_SEARCH_DEPTH = 64;
    public static final int MAX_MOVES_IN_POS = 218;

    protected PositionEncoder.Position pos;                                        // state
    protected final MctsTree tree;                                                 // eval the states and chose with PUCT
    protected final int[] moves = new int[MAX_MOVES_IN_POS];                       // pre-allocated move array to max search depth to avoid rapid allocs. and deallocs. in train-loop
    protected final int[] undoInfoStack = new int[MAX_SEARCH_DEPTH];               // all undoInformation in a stack so it can be accessed easily; access[cDepth - 1]
    protected final float[] moveDist = new float[Move.MOVE_POSSIBILITIES];         // will hold the distributions for all the most likely moves;


    protected final Evaluator evaluator;
    protected int depth = 1;                                                      // depth = 1 da root immer existiert
    protected int playDepth;
    private Stack<Integer> undoMoveStack = new Stack<>();


    /**
     * Initializes a ChessBot with a given pos
     * @param evaluator what evaluation should be used
     * @param fen the fenStr of the starting pos
     * @param playDepth how many MCTS iters a Bot should do
     */
    public ChessBot(Evaluator evaluator, String fen, int playDepth){
        this.evaluator = evaluator;
        this.playDepth = playDepth;
        pos = PositionUtils.fromFen(fen);
        tree = new MctsTree();
    }


    /**
     * Initializes a ChessBot with the starting pos
     * @param evaluator what evaluation should be used
     * @param playDepth how many MCTS iters a Bot should do
     */
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
                tree.w[node] += v.value();
                expand(node, v.policyV());               // add all moves to the end
                tree.n[node]++;
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

        if(out.second() != GameState.Continue) {
            throw new IllegalStateException("Position already a checkmate");
        }

        normalizeDist(policyV, moves, out.first());

        // iterate over all moves in curr pos.
        for (int i = 0; i < out.first(); i++) {
            int child = tree.addNewChild(node, moves[i]);
            tree.p[child] = policyV[PolicyIndex.toPolicyIndex(moves[i])];    // add a new node and set the policy vector
            //tree.p[child] = policyV[PolicyIndex.toPolicyIndex(moves[i])];    // add a new node and set the policy vector
        }
    }

    /**
     * backpropagate a given value from a given node
     * @param val position rating (z)
     */
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
     * output how good every move is being evalled by the MCTS +
     * @param debug also prints n, q, w of each node
     */
    public void outputMoveDist(boolean debug){
        int node = tree.firstChild[0];
        while (node != 0){
            int move = tree.move[node];
            float q = tree.n[node] == 0 ? 0f : tree.w[node] / tree.n[node];             // evaluation
            if (debug) {
                System.out.println("n: " + tree.n[node]);
                System.out.println("q: " + q);
                System.out.println("w: " + tree.w[node]);
                System.out.println("p: " + tree.p[node]);
            }
            String moveStr = toFieldName(Move.from(move)) + toFieldName(Move.to(move));
            float score = q + tree.p[node];
            System.out.printf("moveDist: %s + %d. score: %f\n", moveStr, Move.flags(move), score);
            node = tree.nextSibling[node];
        }
    }

    protected float[] normalizeDist(float[] policyV, int[] moves, int moveOff){
        System.arraycopy(policyV, 0, moveDist, 0, moveDist.length);
        Arrays.fill(policyV, Float.NEGATIVE_INFINITY);
        for (int i = 0; i < moveOff; i++) {
            int idx = PolicyIndex.toPolicyIndex(moves[i]);
            policyV[idx] = moveDist[idx];
        }
        return policyV;
    }


    /**
     * resets values so that a new position can be sent to the tree
     */
    public void resetCore() {
        tree.reset();
        depth = 0;
    }

    /**
     * expand the root with dietrichNoise
     */
    protected void expandRoot(){
        Evaluator.Result r = getEvaluator().evaluate(pos);
        tree.w[ROOT] = r.value();
        tree.n[ROOT] = 1;
        expand(ROOT, r.policyV());
    }

    public void expandRootNoise(){
        expandRoot();
        tree.addNoiseToRootChildren();
    }


    /**
     * play a game and fill a sampleBuffer
     * @param n MCTS movedepth
     * @param startoffset how many moves are in the buffer
     * @param end   how many moves should be in the buffer
     * @param buffer the SampleBuffer
     * @return the new startoffset
     */
    public int selfPlayGame(int n, int startoffset, int end, SampleBuffer buffer){
        float z = 0;
        float[] flat;
        int startPtr = buffer.getPtr();

        GameState g = pos.getState(pos.whiteToMove);
        int move;
        while (g == GameState.Continue && startoffset < end){       // loop until Check/stalemate or full buffer

            flat = PositionEncoder.encodeFlat(pos);     // save pos before move happens
            expandRootNoise();
            move = bestMoveUCB(n);
            buffer.addSample(flat, tree.moveDist(), z); // record the moveDist. and z value
            ConsoleBar.render((double) startoffset /end, 2);
            pos.makeMove(move);

            resetCore();
            g = pos.getState(pos.whiteToMove);
            startoffset ++;
        }
        z = switch (g){
            case Continue -> 0.0F;
            case StaleMate -> -0.5F;
            case CheckMate -> 1;
            case FiftyMove -> -0.8F;
            case Material -> 0.0F;
            case Resignation -> 0.0F;
        };
        buffer.updateZ(startPtr, z);
        return startoffset;
    }


    /**
     * sims a game and outputs to the screen
     * every move will have n rounds in the tree
     */
     public void selfPlayGame(int n){

        GameState g = pos.getState(pos.whiteToMove);

        System.out.println("startGame");
        int move;
        while (g == GameState.Continue){
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
        )), 400, 1);
        bot.selfPlayGame(400);
//        BatchChessBot b = new BatchChessBot(new BatchEvaluator.StubEvaluator());
//        b.selfPlayGame(300);
    }

    // methods for ChessGame

    /**
     * does playdepth simulations and returns the best move as the support MovePacket format
     */
    @Override
    public MovePacket requestMove() {
        int rMove = bestMove(playDepth);
        var r = getEvaluator().evaluate(pos);
        System.out.println("Net rates positions: " + r.value());
        return Move.toMovePacket(rMove);
    }

    /**
     *      the other player played made move and this syncs the local board
     */
    @Override
    public void makeMove(MovePacket movePacket) {
        int move = Move.MovePacketToMove(movePacket);

        undoMoveStack.push(
                pos.makeMove(move)
        );
        resetCore();
    }

    @Override
    public void undoMove() {
        pos.unmakeMove(undoMoveStack.pop());
    }

    @Override
    public void close() throws IOException {
        evaluator.close();
    }
}
