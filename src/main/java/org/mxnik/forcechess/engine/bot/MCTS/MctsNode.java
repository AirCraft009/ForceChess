package org.mxnik.forcechess.engine.bot.MCTS;


/**
 * Class for the node of the MCTS (Monte-Carlo-Tree-Search) algorithm
 * <p>
 * total = total score after rollout
 * visits = number of visits tothe node
 */
public class MctsNode {
    // this term decides the balance between exploration and exploitation
    public static final int Balance = 2;

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


    public float total;
    public int visits;

}
