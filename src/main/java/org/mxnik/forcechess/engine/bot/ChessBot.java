package org.mxnik.forcechess.engine.bot;

import org.mxnik.forcechess.Util.Bitboard;
import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.engine.MCTS.MctsTree;
import org.mxnik.forcechess.engine.Pos.Move;
import org.mxnik.forcechess.engine.Pos.MoveGen;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;

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
    private int[] movePtrStack = new int[MAX_SEARCH_DEPTH + 1];                 // contains the ptr to one above the last entry. Has a 0 entry on 0 idx for simplicity purp. (example: segment 1: 0 - 12; movePtrStack[0] = 13
    private int cDepth = 1;                                                     // current depth pointer 1 - MAX_SEARCH_DEPTH; to get the correct value from the movePtrStack
    public int currentNode = 0;                                                 // start at the root node S0
    public int[] undoInfoStack = new int[MAX_SEARCH_DEPTH];                     // all undoInformation in a stack so it can be accessed easily; access[cDepth - 1]


    public ChessBot(){
        pos = PositionEncoder.Position.StartingPosition();
        tree = new MctsTree();
    }

    /**
     * gets all possible moves and then adds them to the current node as children
     */
    public void addNodes(float[] policyV) {
        // cDepth - 1 to get the last offset
        movePtrStack[cDepth] = MoveGen.generateMoves(pos, movePtrStack[cDepth-1], pos.whiteToMove, moves);
        // iterate over all moves in curr pos.
        for (int i = movePtrStack[cDepth - 1]; i < movePtrStack[cDepth]; i++) {
            tree.p[tree.addNewChild(currentNode, moves[i])] = policyV[i-movePtrStack[cDepth-1]];    // add a new node and set the policy vector
        }
    }

    /**
     * expands the leaf node and
     * sets the policy for all below
     * <p>
     * then backpropagates all values and updates W,N,Q and N_total
     * @param nodeIdx index of the node value;
     */
    public void handleLeafNode(int nodeIdx){
        DiversePair<Float, float[]> output = AlphaNet.runNet(pos);
        float value = output.first();
        float[] policyV = output.second();

        addNodes(policyV);      // create all nodes and set P
        tree.globalVisits++;

        for (int i = 0; i < cDepth; i++) {      // go up the tree and backpropagate up the tree
            tree.w[nodeIdx] += value;
            tree.n[nodeIdx]++;

            value = -value;                     // flip value because colors flip with moves

            nodeIdx = tree.parentIdx[nodeIdx];
        }
    }

    public void doIters(int iter){
        // ensure base state
        currentNode = 0;
        cDepth = 1;
        DiversePair<Float, float[]> output = AlphaNet.runNet(pos);
        addNodes(output.second());
        tree.n[currentNode]++;
        tree.w[currentNode] += output.first();
        tree.globalVisits++;
        for (int i = 0; i < iter; i++) {
            traverseTree();
        }
    }

    /**
     * traverses the tree starting at the currentNode
     * <p>
     * It is expected that the currentNode has childNodes
     */
    public void traverseTree() {
        while (true) {
            if (cDepth == MAX_SEARCH_DEPTH) {
                undoInfoStack[cDepth-1] = pos.makeMove(tree.move[currentNode]);
                handleLeafNode(currentNode);
                // go back to root after passing values back up
                currentNode = 0;
                unmakeAll();
                cDepth = 1;
                return;
            }
            if(tree.firstChild[currentNode] == 0){  // hit unexpanded leaf-node
                //undoInfoStack[cDepth-1] = pos.makeMove(tree.move[currentNode]); // make move
                handleLeafNode(currentNode);
                // go back to root after passing values back up
                currentNode = 0;
                //System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
                unmakeAll();
                cDepth = 1;
                //System.out.println("ending iter");
                return;
            }

            currentNode = tree.findBestChild(currentNode);
            //System.out.println(pos.whiteToMove);
            undoInfoStack[cDepth-1] = pos.makeMove(tree.move[currentNode]);
            //System.out.printf("moving: from %d -> to %d\n", Move.from(tree.move[currentNode]), Move.to(tree.move[currentNode]));
            cDepth ++;
        }
    }

    public void unmakeAll(){
        cDepth--;   // set the depth pointer to be the ptr
        while (cDepth > 0) {
            cDepth--;
            pos.unmakeMove(undoInfoStack[cDepth]);
        }
    }


    public static void main(String[] args) {
        ChessBot bot = new ChessBot();
        bot.doIters(10000);
        bot.unmakeAll();
        //System.out.printf("%d->%d\n", Move.from(bot.tree.move[bot.tree.findBestChild(0)]), Move.to(bot.tree.move[bot.tree.findBestChild(0)]));
        //System.out.println(bot.movePtrStack[1]);
    }
}
