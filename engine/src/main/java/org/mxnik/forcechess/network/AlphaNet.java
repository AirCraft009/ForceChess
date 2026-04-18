package org.mxnik.forcechess.network;

import org.deeplearning4j.nn.graph.ComputationGraph;
import org.mxnik.forcechess.Pos.PositionEncoder;
import org.mxnik.forcechess.bot.Evaluator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * The neural net used for evaluating position and returning the bestMove
 */
public final class AlphaNet implements Evaluator {
    private final ComputationGraph model;
    private float[] flat = new float[PositionEncoder.PLANES * PositionEncoder.PLANE_SIZE];

    /**
     * Net Evaluator
     * @param model uninitialized model
     */
    public AlphaNet(ComputationGraph model) {
        this.model = model;
        model.init();
    }

    public ComputationGraph getModel(){
        return model;
    }

    @Override
    public Result evaluate(PositionEncoder.Position pos) {
        // encode position in flat array
        PositionEncoder.encode(pos, flat);
        INDArray input = Nd4j.create(flat, new int[]{1, PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE});
        INDArray[] out = model.output(false, input);
        float[] policy = out[0].toFloatVector();
        float positionRating = out[1].getFloat(0);
        for (INDArray o : out){
            o.close();
        }
        input.close();
        return new Result(policy, positionRating);
    }

    // because flat is still filled after evaluate and needed as input for TrainingSamples
    public float[] getFlatCopy(){
        return flat.clone();
    }




}
