package org.mxnik.forcechess.network;

import org.deeplearning4j.nn.conf.WorkspaceMode;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.mxnik.forcechess.Pos.PositionEncoder;
import org.mxnik.forcechess.bot.BatchChessBot;
import org.mxnik.forcechess.bot.BatchEvaluator;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.memory.conf.WorkspaceConfiguration;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.Closeable;
import java.io.IOException;

/**
 * The neural net used for evaluating position and returning the bestMoveUCB
 */
public final class AlphaNet implements BatchEvaluator, Closeable {
    private final ComputationGraph model;
    private final float[] flat;
    private final INDArray input = Nd4j.create(
            new int[]{BatchChessBot.BATCH_SIZE, PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE}, DataType.FLOAT16);


    /**
     * Net Evaluator & Batchevaluator leveraging a given model by giving it the ability to use batching and normal interference
     * @param model initialized model
     */
    public AlphaNet(ComputationGraph model) {
        this.model = model;
        model.getConfiguration().setInferenceWorkspaceMode(WorkspaceMode.ENABLED);
        Nd4j.getMemoryManager().setAutoGcWindow(5000);
        flat = new float[PositionEncoder.PLANES * PositionEncoder.PLANE_SIZE];

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

    /**
     * evaluates a batch that are all encoded into a single big array
     * @param inputs TENSOR_SIZE * BATCHSIZE encoded positions
     * @return policy-head , and position rating
     */
    @Override
    public Result[] evaluateBatch(float[] inputs) {
        Result[] results = new Result[BatchChessBot.BATCH_SIZE];

        input.data().setData(inputs);
        INDArray[] out = model.output(false, input);

        for (int i = 0; i < BatchChessBot.BATCH_SIZE; i++) {
            results[i] = new Result(out[0].getRow(i).toFloatVector(), out[1].getFloat(i));
        }

        out[0].close();
        out[1].close();

        return results;
    }

    public int bestMove(PositionEncoder.Position inPos){
        PositionEncoder.encode(inPos, flat);
        INDArray input = Nd4j.create(flat, new int[]{1, PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE});
        INDArray[] out = model.output( false, input);
        float[] policy = out[0].toFloatVector();
        int maxMove = -1;
        float maxS = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < policy.length; i++) {
            if(maxS < policy[i]){
                maxMove = i;
                maxS = policy[i];
            }
        }
        return maxMove;
    }


    @Override
    public void close() throws IOException {
        input.close();
    }
}
