package org.mxnik.forcechess.engine.network;


import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.engine.Pos.Move;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;
import org.mxnik.forcechess.engine.bot.ChessBot;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;

public class Train {
    private ChessBot bot;
    private AlphaNet network;
    private String fullPath;
    private int checkPointC = 0;
    public final static String basePath = "boardsNBots/bots/";

    /**
     * Read the configured AI-model from the file specified (no file ending)
     */
    public Train(String fileName) throws IOException {
        this(fileName, true);
    }

    private Train(String fileName, boolean read) throws IOException {
        fullPath = basePath + fileName;
        if (!read) {
            network = new AlphaNet(NetworkConfig.buildNet());
            bot = new ChessBot(network);
            return;
        }

        ComputationGraph loaded = ModelSerializer.restoreComputationGraph(
                new File(fullPath), true
        );
        network = new AlphaNet(loaded);
        bot  = new ChessBot(network);
    }

    /**
     * creates a Train instances with the given network that will be saved to fileName
     */
    public Train(AlphaNet net, String fileName) {
        fullPath = basePath + fileName;
        network = net;
        bot = new ChessBot(net);
    }

    public void saveCheckPoint(){
        String checkPath = fullPath + "_" + checkPointC + "_checkPoint.zip";
    }

    /**
     * trains a NeuralNetwork
     * @param size amount of moves played in total over all games
     * @param moveDepth how often the MCTS-Loop is run for each move
     * @param checkPoint how many batches have to be played till a checkpoint is saved <p></p>
     *                   - checkpoints are set as filename_n_checkPoint.zip
     */
    public SampleBuffer fillBuffer(int size, int moveDepth, int checkPoint) {
        return  new SampleBuffer(1);
    }

    private void trainFromBuffer(int batchSize, SampleBuffer buffer){
        // SETUP
        // each sample: one position + its pi + its z
        // inputs: [batchSize, 19, 8, 8]
        INDArray inputs = Nd4j.zeros(batchSize, PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE);
        // policy targets: [batchSize, 65536] (the pi distribution)
        INDArray piTargets = Nd4j.zeros(batchSize, Move.MOVE_POSSIBILITIES);
        // value targets: [batchSize, 1] (the game outcome z)
        INDArray zTargets = Nd4j.zeros(batchSize, 1);

        // PLAY_GAMES & COLLECT_SAMPLES


        for (int i = 0; i < batchSize; i++) {
            TrainingSample s = buffer.sample();
            inputs.putRow(i, Nd4j.create(s.tensor(), new int[]{1, 19, 8, 8}, 'c'));
            piTargets.putRow(i, Nd4j.create(s.pi()));
            zTargets.putRow(i, Nd4j.create(new float[]{s.z()}));
        }
    }
}
