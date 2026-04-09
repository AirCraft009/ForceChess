package org.mxnik.forcechess.engine.bot;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.mxnik.forcechess.engine.Pos.Move;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * The neural net used for evaluating position and returning the bestMove
 */
public final class AlphaNet implements Evaluator{
    private final ComputationGraph model;
    private float[] flat = new float[PositionEncoder.PLANES * PositionEncoder.PLANE_SIZE];

    public AlphaNet(ComputationGraph model) {
        this.model = model;
    }

    @Override
    public Result evaluate(PositionEncoder.Position pos) {
        // encode position in flat array
        PositionEncoder.encode(pos, flat);
        INDArray input = Nd4j.create(new int[]{1, 19, 8, 8}, flat);
        INDArray[] out = model.output(input);
        float[] policy = out[0].toFloatVector();
        float positionRating = out[1].getFloat(0);
        for (INDArray o : out){
            o.close();
        }
        input.close();
        return new Result(policy, positionRating);
    }


}
