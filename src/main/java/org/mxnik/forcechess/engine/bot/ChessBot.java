package org.mxnik.forcechess.engine.bot;

import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.engine.MCTS.MctsTree;
import org.mxnik.forcechess.engine.Pos.MoveGen;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;

import java.util.FormattableFlags;
import java.util.function.Function;

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
    public void addNodes() {
        // cDepth - 1 to get the last offset
        movePtrStack[cDepth] = MoveGen.generateMoves(pos, movePtrStack[cDepth-1], pos.whiteToMove, moves);
        // iterate over all moves in curr pos.
        for (int i = movePtrStack[cDepth - 1]; i < movePtrStack[cDepth]; i++) {
            tree.addNewChild(currentNode, moves[i]);
        }
    }

    /**
     * handles a leaf node and returns the values up the tree
     * @param nodeIdx index of the node value;
     */
    public void handleLeafNode(int nodeIdx){
        DiversePair<Float, float[]> output = AlphaNet.runNet(pos, tree.move[nodeIdx]);
        float value = output.first();
        float[] policyV = output.second();
        tree.p[nodeIdx] = policyV[0];           // select correct values from policy vector

        for (int i = 0; i < cDepth; i++) {
            tree.w[nodeIdx] += value;
            tree.n[nodeIdx] += 1;

            nodeIdx = tree.parentIdx[nodeIdx];
        }
    }

    public void doIters(int iter){
        // ensure base state
        currentNode = 0;
        cDepth = 1;
        for (int i = 0; i < iter; i++) {
            traverseTree();
        }
    }

    /**
     * traverses the tree starting at the currentNode
     * <p>
     * It is expected that the currentNode has childNodes
     */
    public void traverseTree(){
        while(true) {
            if (tree.firstChild[currentNode] == 0)       // no children yet
                addNodes();

            int child = tree.findBestChild(currentNode);
            if (tree.n[child] == 0) {         // first time visiting/is a leaf node
                handleLeafNode(child);
                // go back to root after passing values back up
                currentNode = 0;
                unmakeAll();
                cDepth = 1;
                return;
            }
            currentNode = child;
            undoInfoStack[cDepth-1] = pos.makeMove(tree.move[currentNode]);
            cDepth++;
        }
    }

    public void unmakeAll(){
        while (cDepth > 0) {
            cDepth--;   // set the depth pointer to be the ptr
            pos.unmakeMove(undoInfoStack[cDepth]);
        }
    }


    public static void main(String[] args) {
        ChessBot bot = new ChessBot();
        bot.doIters(MAX_SEARCH_DEPTH);
        //System.out.println(Arrays.toString(Arrays.copyOf(bot.moves, 40)));
        //System.out.println(bot.movePtrStack[1]);
    }
}
