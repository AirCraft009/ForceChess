package org.mxnik.forcechess.bot.botComponents;

public class Node {
    private double bias;
    private Node[] parents;
    private double[] weights;

    private double activation;

    public Node(double bias, Node[] parents, double[] weights) {
        this.bias = bias;
        this.parents = parents;
        this.weights = weights;
    }
    Node(double activation){
        this.bias = 0;
        this.parents = new Node[0];
        this.weights = new double[0];
        this.activation = activation;
    }

    public void calculate() {
        double sum = bias;
        for(int i = 0; i < parents.length; i++)
            sum += weights[i] * parents[i].getActication();
        activation = Double.max(0, sum);
    }

    public double getActication(){
        return activation;
    }

    public static void main(String[] args) {
        Node[] nodes = {new Node(0.5), new Node(0.8)};
        double[] weights = {.4, .6};
        Node n = new Node(.1, nodes, weights);
        n.calculate();
        System.out.println(n.getActication());
    }
}
