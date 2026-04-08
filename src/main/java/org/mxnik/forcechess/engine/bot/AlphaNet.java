package org.mxnik.forcechess.engine.bot;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
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
    private static final Activation USED_ACTIVATION = Activation.RELU;
    private static final boolean HAS_BIAS = false;           // bias useless with batch-normalization (why not have easier control tho)
    private static final double LEARNING_RATE = 1e-3;
    private static final int SEED = 67;


    // ResNet Layer names
    private static final String RES_BASE = "resBlock";
    private static final String conv1 = RES_BASE + "-conv1-";        // first convolutional layer
    private static final String conv2 = RES_BASE + "-conv2-";        // second conv layer in resNet
    private static final String batch1 = RES_BASE + "-bn1-";           // first batch normalisation
    private static final String batch2 = RES_BASE + "-bn2-";           // batch normalisation
    private static final String act = RES_BASE + "-activation";         // activation layer (RELU)
    private static final String addition = RES_BASE + "-activation";     // Vertex for adding x; F(x) + x, F(x) = H(x) - x
    private static final String out = RES_BASE + "-out";                // output


    public void buildNet(){
        org.deeplearning4j.nn.conf.ComputationGraphConfiguration.GraphBuilder graphBuilder;

        NeuralNetConfiguration.Builder confBuilder = new NeuralNetConfiguration.Builder()
                .seed(SEED)                                                     // seed for random values
                .updater(new Adam(LEARNING_RATE))                               // how updates are applied Adam updater takes learningRate
                .weightInit(WeightInit.RELU);                                   // helps with vanishing gradient
        graphBuilder = confBuilder.graphBuilder();

        conf = graphBuilder.addInputs("board")                              // position tensor.
                .addLayer("conv-base",
                new ConvolutionLayer.Builder(CONV_LAYER_SIZE, CONV_LAYER_SIZE)  // 3x3 conv layer
                    .nIn(PositionEncoder.PLANES).nOut(CONV_OUT)                 // layer input and throughput
                    .padding(PADDING, PADDING)                                  // how spacial dim changes
                    .activation(Activation.IDENTITY)
                    .hasBias(HAS_BIAS)
                    .build(), "board")                                          // gets input from board
                .addLayer("batch-normalization-base",
                new BatchNormalization.Builder().nOut(CONV_OUT).build(), "conv-base")    //input inherited from base layer same output just norm.
                .addLayer("activation-base",
                new ActivationLayer(USED_ACTIVATION), "batch-normalization-base")        // add RELU activation layer (inputs = batch norm.)
                // add ResNet blocks.
                .setOutputs("policy", "value")                                           // policy vector(each move), single value rating position
                .build();
    }

    public void addResNetBlocks(org.deeplearning4j.nn.conf.ComputationGraphConfiguration.GraphBuilder gb, int n, String prevLayerName){
        String inputLayer = prevLayerName;
        for (int i = 0; i < n; i++) {
            gb.addLayer(conv1 + n, new ConvolutionLayer.Builder(3,3)
                    .nIn(256).nOut(256).padding(1,1)
                    .activation(Activation.IDENTITY).hasBias(false)
                    .build(), inputLayer)
            .addLayer(batch1 + n, new BatchNormalization.Builder().nOut(256).build(), conv1 + n)
            .addLayer(act + n, new ActivationLayer(USED_ACTIVATION), batch1)
            .addLayer(conv2 + n, new ConvolutionLayer.Builder(3,3)
                    .nIn(256).nOut(256).padding(1,1)
                    .activation(Activation.IDENTITY).hasBias(false)
                    .build(), act + n)
            .addLayer(batch2+n, new BatchNormalization.Builder().nOut(256).build(), conv2)

            // Vertex to add x back ( residual net)
            // y = F(x) + x;
            .addVertex(addition + n, new ElementWiseVertex(ElementWiseVertex.Op.Add),
                    inputLayer,batch2+n)
            .addLayer(out+n, new ActivationLayer(USED_ACTIVATION), addition + n);

            inputLayer = out+n;
        }



    }

    @Override
    public Result evaluate(PositionEncoder.Position pos) {
        return null;
    }


}
