package org.mxnik.forcechess.engine.bot;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.ActivationLayer;
import org.deeplearning4j.nn.conf.layers.BatchNormalization;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;

/**
 * The neural net used for evaluating position and returning the bestMove
 */
public class AlphaNet implements Evaluator{
    private ComputationGraphConfiguration conf;
    private static final int CONV_LAYER_SIZE = 3;
    private static final int CONV_OUT = 256;                // higher dim. channels (maybe lower, if it takes too long)
    private static final int PADDING = 1;                 // how spacial layers will be changed (1 = no change = 8x8 layers)
    private static final Activation USED_ctivation = Activation.IDENTITY;           // linear activation (identity) = none; will play around
    private static final boolean HAS_BIAS = false;           // bias useless with batch-normalization (why not have easier control tho)
    private static final double LEARNING_RATE = 1e-3;
    private static final int SEED = 67;

    public void buildNet(){
        ComputationGraphConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(SEED)                                                     // seed for random values
                .updater(new Adam(LEARNING_RATE))                               // how updates are applied Adam updater takes learningRate
                .weightInit(WeightInit.RELU)                                    // helps with vanishing gradient
                .graphBuilder()
                .addInputs("board")                                 // position tensor.
                .addLayer("conv-base",
                new ConvolutionLayer.Builder(CONV_LAYER_SIZE, CONV_LAYER_SIZE)  // 3x3 conv layer
                    .nIn(PositionEncoder.PLANES).nOut(CONV_OUT)                 // layer input and throughput
                    .padding(PADDING, PADDING)                                  // how spacial dim changes
                    .activation(USED_ctivation)                                 // prob. always linear
                    .hasBias(HAS_BIAS)
                    .build(), "board")                               // gets input from board
                .addLayer("batch-normalization-base",
                new BatchNormalization.Builder().nOut(CONV_OUT).build(), "conv-base")    //input inherited from base layer same output just norm.
                .addLayer("relu-activation-base",
                new ActivationLayer(Activation.RELU), "batch-normalization-base")        // add RELU activation layer (inputs = batch norm.)
                .setOutputs("policy", "value")                                  // policy vector(each move), single value rating position
                .build();
    }

    @Override
    public Result evaluate(PositionEncoder.Position pos) {
        return null;
    }


}
