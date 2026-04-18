package org.mxnik.forcechess.network;

import org.deeplearning4j.nn.conf.ComputationGraphConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.PositionEncoder;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public final class NetworkConfig {

    // --- Architecture constants ---
    private static final int KERNEL        = 3;
    private static final int CONV_OUT      = 256;
    private static final int PAD           = 1;
    public static final int RES_BLOCKS    = 20;

    // Policy head
    private static final int POL_CHANNELS  = 32;
    private static final int POL_HIDDEN    = 128;

    // Value head
    private static final int VAL_HIDDEN    = 64;

    // Training
    private static final double LR         = 1e-3;
    private static final int    SEED       = 41;

    // Activations / losses
    private static final Activation                 ACT        = Activation.RELU;
    private static final LossFunctions.LossFunction POL_LOSS = LossFunctions.LossFunction.MCXENT;
    private static final LossFunctions.LossFunction VAL_LOSS = LossFunctions.LossFunction.MSE;

    // ResBlock layer-name prefixes
    private static final String C1  = "rb-c1-";
    private static final String C2  = "rb-c2-";
    private static final String BN1 = "rb-bn1-";
    private static final String BN2 = "rb-bn2-";
    private static final String A1  = "rb-act1-";
    private static final String ADD = "rb-add-";
    private static final String OUT = "rb-out-";


    public static ComputationGraph buildNet() {

        ComputationGraphConfiguration.GraphBuilder g =
                new NeuralNetConfiguration.Builder()
                        .seed(SEED)
                        .updater(new Adam(LR))
                        .weightInit(WeightInit.RELU)          // He init for hidden layers
                        .graphBuilder();


        g.addInputs("board")

                .addLayer("stem-conv",
                        new ConvolutionLayer.Builder(KERNEL, KERNEL)
                                .nIn(PositionEncoder.PLANES).nOut(CONV_OUT)
                                .padding(PAD, PAD)
                                .activation(Activation.IDENTITY)
                                .hasBias(false)
                                .build(), "board")

                .addLayer("stem-bn",
                        new BatchNormalization.Builder().nOut(CONV_OUT).build(), "stem-conv")

                .addLayer("stem-act",
                        new ActivationLayer(ACT), "stem-bn");

        // residual blocks
        String towerOut = addResBlocks(g, RES_BLOCKS, "stem-act");

        // Policy Vector output
        //
        //  towerOut -> conv(1×1, 256→32) → BN → ReLU
        //           -> [CnnToFF preprocessor] → dense(2048→128) → softmax(MOVE_POSSIBILITIES)
        //
        g.inputPreProcessor("pol-flat", new CnnToFeedForwardPreProcessor(8, 8, POL_CHANNELS))

                .addLayer("pol-conv",
                        new ConvolutionLayer.Builder(1, 1)
                                .nIn(CONV_OUT).nOut(POL_CHANNELS)
                                .activation(Activation.IDENTITY).hasBias(false)
                                .build(), towerOut)

                .addLayer("pol-bn",
                        new BatchNormalization.Builder().nOut(POL_CHANNELS).build(), "pol-conv")

                .addLayer("pol-act",
                        new ActivationLayer(ACT), "pol-bn")

                // flatten here via inputPreProcessor registered above
                .addLayer("pol-flat",
                        new DenseLayer.Builder()
                                .nIn((long) POL_CHANNELS * 8 * 8).nOut(POL_HIDDEN)
                                .weightInit(WeightInit.XAVIER)       // Xavier for the dense bridge
                                .activation(ACT)
                                .hasBias(true)
                                .build(), "pol-act")

                .addLayer("policy",
                        new OutputLayer.Builder(POL_LOSS)
                                .nIn(POL_HIDDEN).nOut(Move.MOVE_POSSIBILITIES)
                                .weightInit(WeightInit.XAVIER)       // Xavier on output — avoids softmax collapse
                                .activation(Activation.SOFTMAX)
                                .hasBias(true)
                                .build(), "pol-flat");

        // Value Head

        //  towerOut -> conv(1×1, 256→1) -> BN -> ReLU
        //           -> GlobalAvgPool (1) -> dense(1→64) -> dense(64→1) -> tanh

        g.addLayer("val-conv",
                        new ConvolutionLayer.Builder(1, 1)
                                .nIn(CONV_OUT).nOut(1)
                                .activation(Activation.IDENTITY).hasBias(false)
                                .build(), towerOut)

                .addLayer("val-bn",
                        new BatchNormalization.Builder().nOut(1).build(), "val-conv")

                .addLayer("val-act",
                        new ActivationLayer(ACT), "val-bn")

                .addLayer("val-pool",
                        new GlobalPoolingLayer.Builder()
                                .poolingType(PoolingType.AVG)
                                .build(), "val-act")

                // scalar (1) → small hidden → output
                // hasBias=true + Xavier prevents tanh saturation at init
                .addLayer("val-dense",
                        new DenseLayer.Builder()
                                .nIn(1).nOut(VAL_HIDDEN)
                                .weightInit(WeightInit.XAVIER)
                                .activation(ACT)
                                .hasBias(true)
                                .build(), "val-pool")

                .addLayer("value",
                        new OutputLayer.Builder(VAL_LOSS)
                                .nIn(VAL_HIDDEN).nOut(1)
                                .weightInit(WeightInit.XAVIER)
                                .activation(Activation.TANH)
                                .hasBias(true)
                                .build(), "val-dense");

        // Outputs
        ComputationGraphConfiguration conf = g
                .setOutputs("policy", "value")
                .build();

        ComputationGraph comp = new ComputationGraph(conf);
        comp.init();
        for (int i = 0; i < RES_BLOCKS; i++) {
            String paramKey = "rb-c2-" + i + "_W";
            comp.getParam(paramKey).assign(0.0).close();
        }
        return comp;
    }




    /**
     * Appends {@code n} residual blocks to {@code gb}.
     * Each block: Conv->BN->ReLU->Conv->BN -> add(skip) → ReLU
     *
     * @param gb            graph builder
     * @param n             number of blocks
     * @param prevLayer     name of the layer feeding into block 0
     * @return              name of the last output layer
     */
    public static String addResBlocks(
            ComputationGraphConfiguration.GraphBuilder gb,
            int n,
            String prevLayer) {

        String input = prevLayer;

        for (int i = 0; i < n; i++) {

            gb.addLayer(C1 + i,
                            new ConvolutionLayer.Builder(KERNEL, KERNEL)
                                    .nIn(CONV_OUT).nOut(CONV_OUT).padding(PAD, PAD)
                                    .activation(Activation.IDENTITY).hasBias(false)
                                    .build(), input)

                    .addLayer(BN1 + i,
                            new BatchNormalization.Builder().nOut(CONV_OUT).build(), C1 + i)

                    .addLayer(A1 + i,
                            new ActivationLayer(ACT), BN1 + i)

                    .addLayer(C2 + i,
                            new ConvolutionLayer.Builder(KERNEL, KERNEL)
                                    .nIn(CONV_OUT).nOut(CONV_OUT).padding(PAD, PAD)
                                    .activation(Activation.IDENTITY).hasBias(false)
                                    .build(), A1 + i)

                    .addLayer(BN2 + i,
                            new BatchNormalization.Builder().nOut(CONV_OUT).build(), C2 + i)

                    // F(x) + x
                    .addVertex(ADD + i,
                            new ElementWiseVertex(ElementWiseVertex.Op.Add),
                            input, BN2 + i)

                    .addLayer(OUT + i,
                            new ActivationLayer(ACT), ADD + i);

            input = OUT + i;
        }

        return input;
    }
}