package org.mxnik.forcechess.engine.bot.MCTS;

public class MctsTree {
    // balances exploitation with exploration
    public static final int Balance = 2;
    // max amount of MctsNodes
    private static final int POOL_SIZE = 500000;

    // parallel arrays — one slot per node
    private final int[]   parentIdx    = new int[POOL_SIZE];
    private final int[]   firstChild   = new int[POOL_SIZE]; // index of first child
    private final int[]   nextSibling  = new int[POOL_SIZE]; // linked list of children
    private final int[]   move         = new int[POOL_SIZE];
    private final float[] w            = new float[POOL_SIZE];
    private final float[] p            = new float[POOL_SIZE];
    private final int[]   n            = new int[POOL_SIZE];

    private int nextFree = 1; // 0 = root
    private int globalVisits; // total number of visits over all nodes.



    /**
     * Decides what nodes are most worth exploring
     * vi + C * sqrt ( ln(N) / ni)
     * @param total the total of the node currently in focus
     * @param specificVisits amount of times the current node has been visited <p> if zero UCB1 value is maximized
     * @param globalVisits general amount of visits / iterations
     * @return the value of the current node; higher is better
     */
    public static double UCB1(float total, int specificVisits, int globalVisits){
        return (specificVisits == 0)? Double.MAX_VALUE : total + Balance * Math.sqrt((Math.log(globalVisits) / specificVisits));
    }

    public void addNewChild(int i, int move){
        int newNode = nextFree++;
        parentIdx[newNode]   = i;
        this.move[newNode]        = move;
        nextSibling[newNode] = firstChild[i]; // prepend to list
        firstChild[i]        = newNode;
    }


}
