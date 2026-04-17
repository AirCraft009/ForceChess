package org.mxnik.forcechess.engine.network;


import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.engine.Pos.Move;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;
import org.mxnik.forcechess.engine.bot.ChessBot;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;

public class Train {
    private final ChessBot bot;
    private final AlphaNet network;
    private final String fullPath;
    private final String fileName;
    private int checkPointC = 0;
    public final static String BASE_PATH = "boardsNBots/bots/networks/";

    /**
     * Read the configured AI-model from the file specified (no file ending)
     */
    public Train(String fileName) throws IOException {
        this(fileName, true);
    }

    private Train(String fileName, boolean read) throws IOException {
        fullPath = BASE_PATH + fileName;
        this.fileName = fileName;
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
        fullPath = BASE_PATH + fileName;
        this.fileName = fileName;
        network = net;
        bot = new ChessBot(net);
    }

    public void saveCheckPoint() throws IOException {
        String checkPath = fullPath + "_" + checkPointC + "_checkPoint.zip";
        ModelSerializer.writeModel(network.getModel(), new File(checkPath), true);
        checkPointC++;
    }

    public void saveNet() throws IOException {
        ModelSerializer.writeModel(network.getModel(), new File(fullPath + ".zip"), true);
    }

    /**
     * plays games against itself and returns a SampleBuffer for training
     *
     * @param size amount of moves played in total over all games
     * @param moveDepth how often the MCTS-Loop is run for each move
     */
    public SampleBuffer selfPlayGames(int size, int intermediate, int moveDepth) {
        SampleBuffer buffer = new SampleBuffer(size, fileName + "_buffer");
        for (int i = 0; i < size; i++) {
            i = bot.selfPlayGame(moveDepth, i, size, intermediate, buffer);
        }
        System.out.println("Finished self play");
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
            inputs.putSlice(i, Nd4j.create(s.tensor, new int[]{PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE}, 'c'));
            piTargets.putRow(i, Nd4j.create(s.pi));
            zTargets.putRow(i, Nd4j.create(new float[]{s.z}));
        }

        network.getModel().fit(new MultiDataSet(
                new INDArray[] {inputs},
                new INDArray[] {piTargets, zTargets}
        ));

        double scores = network.getModel().score();
        System.out.printf("policy loss: %.4f\n", scores);
    }

    /**
     *
     * Trains the AI-model after playing selfPlayGames
     *
     * @param batchSize how big the batch is that the training is used on
     * @param SampleBufferSize amount of positions in the SampleBuffer from the self-play
     * @param n how many MCTS evals per move
     * @param epoch how many times the net will be trained (uses the same sample buffer)
     * @param checkPoint how many batches have to be played till a checkpoint is saved <p></p>
     *                   - checkpoints are set as filename_n_checkPoint.zip
     */
    public void train(int batchSize, int SampleBufferSize, int n, int epoch, int sampleCheckPoint, int checkPoint) throws IOException {
        SampleBuffer b = selfPlayGames(SampleBufferSize, sampleCheckPoint, n);
        train(batchSize, b, epoch, checkPoint);
    }

    /**
     * Trains the AI-model with a given SampleBuffer
     *
     * @param batchSize how big the batch is that the training is used on
     * @param buffer the buffer used to train with
     * @param epoch how many times the net will be trained (uses the same sample buffer)
     * @param checkPoint how many batches have to be played till a checkpoint is saved <p></p>
     *                   - checkpoints are set as filename_n_checkPoint.zip
     */
    public void train(int batchSize, SampleBuffer buffer, int epoch, int checkPoint) throws IOException {
        for (int i = 1; i < epoch + 1; i++) {
            trainFromBuffer(batchSize, buffer);
            if(i % checkPoint == 0){
                saveCheckPoint();
            }
        }
    }


    public static void main(String[] args) throws IOException {
        Train train = new Train(new AlphaNet(NetworkConfig.buildNet()), "D250_T1");
        train.train(32, 1600, 250, 300,100, 10);
        train.saveNet();
    }
}
