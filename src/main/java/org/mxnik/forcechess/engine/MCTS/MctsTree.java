package org.mxnik.forcechess.engine.MCTS;

public final class MctsTree {
    // balances exploitation with exploration
    public static final int C_PUCT = 2;
    // max amount of MctsNodes
    public static final int POOL_SIZE = 500000;

    // parallel arrays — one slot per node
    public final int[]   parentIdx    = new int[POOL_SIZE]; // index of node above
    public final int[]   firstChild   = new int[POOL_SIZE]; // index of first child
    public final int[]   nextSibling  = new int[POOL_SIZE]; // linked list of children (nextSibling[node] == 0 if there are no more)
    public final int[]   move         = new int[POOL_SIZE]; // moves for each position
    public final float[] w            = new float[POOL_SIZE];
    public final float[] p            = new float[POOL_SIZE];
    public final int[]   n            = new int[POOL_SIZE];

    public int nextFree = 1; // 0 = root
    public int globalVisits; // total number of visits over all nodes.

    /**
     * adds a child to a node
     * @param index the node to add the child to
     * @param move the move this node repr
     */
    public void addNewChild(int index, int move){
        int newNode = nextFree++;
        parentIdx[newNode]   = index;
        this.move[newNode]        = move;
        nextSibling[newNode] = firstChild[index]; // prepend to list
        firstChild[index]        = newNode;
    }

    public int findBestChild(int nodeIdx){
        int bestChild = -1;
        float bestScore = Float.NEGATIVE_INFINITY;      // start with the lowest score
        int child = firstChild[nodeIdx];
        float sqrtN = (float) Math.sqrt(n[nodeIdx]);

        while (child != 0) {
            float q = n[child] == 0 ? 0f : w[child] / n[child];             // evaluation
            float score = q + C_PUCT * p[child] * sqrtN / (1 + n[child]);   // get the PUCT score
            if (score > bestScore) {
                bestScore = score;  // update bestScore
                bestChild = child;  // update bestChild
            }
            child = nextSibling[child];
        }
        return bestChild;
    }

    public void reset(){
        nextFree = 1;       // set the ptr back to the start of the list
    }
    
}
