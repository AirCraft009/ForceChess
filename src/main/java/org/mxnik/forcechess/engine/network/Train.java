package org.mxnik.forcechess.engine.network;


import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.engine.Pos.Move;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;
import org.mxnik.forcechess.engine.bot.ChessBot;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;

public class Train {
    private ChessBot bot;
    private AlphaNet network;
    private String fullPath;
    public final static String basePath = "boardsNBots/bots/";

    public Train(String fileName) throws IOException {
        this(fileName, false);
    }

    public Train(String fileName, boolean read) throws IOException {
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

    public Train(AlphaNet net){
        network = net;
        bot = new ChessBot(net);
    }

    /**
     * trains a NeuralNetwork
     * @param size amount of moves played in total over all games
     * @param moveDepth how often the MCTS-Loop is run for each move
     */
    public SampleBuffer selfPlayGames(int size, int moveDepth) {
        SampleBuffer buffer = new SampleBuffer(size);
        for (int i = 0; i < size; i++) {
            bot.selfPlayGame(moveDepth, buffer);
        }
        return buffer;
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
            inputs.putSlice(i, Nd4j.create(s.tensor(), new int[]{PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE}, 'c'));
            piTargets.putRow(i, Nd4j.create(s.pi()));
            zTargets.putRow(i, Nd4j.create(new float[]{s.z()}));
        }

        network.getModel().fit(new MultiDataSet(
                new INDArray[] {inputs},
                new INDArray[] {piTargets, zTargets}
        ));

        double scores = network.getModel().score();
        System.out.printf("policy loss: %.4f\n",
                scores);
    }

    /**
     * @param checkPoint how many batches have to be played till a checkpoint is saved <p></p>
     *                   - checkpoints are set as filename_n_checkPoint.zip
     */
    public void train(int checkPoint){
        SampleBuffer b = selfPlayGames(1, 300);
        trainFromBuffer(10, b);
    }

    public static void main(String[] args) {
        Train train = new Train(new AlphaNet(NetworkConfig.buildNet()));
        train.train(10);
    }
}
