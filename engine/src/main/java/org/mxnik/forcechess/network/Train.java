package org.mxnik.forcechess.network;


import com.sun.jna.platform.unix.X11;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.FileLocations;
import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.PositionEncoder;
import org.mxnik.forcechess.bot.BatchChessBot;
import org.mxnik.forcechess.bot.ChessBot;
import org.mxnik.forcechess.bot.Evaluator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Train {
    private final ChessBot bot;
    private final AlphaNet network;
    private final String fullPath;
    private final String fileName;
    private int checkPointC = 0;
    public final static String BASE_PATH = FileLocations.NETWORK_LOCATIONS;
    public final static String FILE_ENDING = ".zip";

    /**
     * Read the configured AI-model from the file specified (no file ending)
     */
    public Train(String fileName) throws IOException {
        this(fileName, true, false);
    }

    /**
     * Reads the model from the file specified (no extensions)
     * @param fileName file without file-extension(.zip)
     * @param batch use Batched MCTS
     */
    public Train(String fileName, boolean batch) throws IOException {
        this(fileName, true, batch);
    }

    private Train(String fileName, boolean read, boolean batch) throws IOException {
        fullPath = BASE_PATH + fileName;
        this.fileName = fileName;
        if (!read) {
            network = new AlphaNet(NetworkConfig.buildNet());
            bot = batch ? new BatchChessBot(network, 300) : new ChessBot(network, 300);
            return;
        }

        ComputationGraph loaded = ModelSerializer.restoreComputationGraph(
                new File(fullPath + FILE_ENDING), true
        );
        network = new AlphaNet(loaded);
        bot  = batch ? new BatchChessBot(network, 300) : new ChessBot(network, 300);
    }

    /**
     * creates a Train instances with the given network that will be saved to fileName
     */
    public Train(AlphaNet net, String fileName, boolean batch) {
        fullPath = BASE_PATH + fileName;
        this.fileName = fileName;
        network = net;
        bot = batch ? new BatchChessBot(net, 300) : new ChessBot(net, 300);
    }

    /**
     * creates a Train instances with the given ChessBot
     */
    public Train(ChessBot bot, String fileName) {
        fullPath = BASE_PATH + fileName;
        this.fileName = fileName;

        Evaluator e = bot.getEvaluator();
        if(e.getClass() != AlphaNet.class){
            throw new IllegalArgumentException("Only Bots with an AlphaNet can be passed to the train method");
        }
        this.network = (AlphaNet) e;
        this.bot = bot;
    }

    public void saveCheckPoint() throws IOException {
        String checkPath = fullPath + "_" + checkPointC + "_checkPoint" + FILE_ENDING;
        ModelSerializer.writeModel(network.getModel(), new File(checkPath), true);
        checkPointC++;
    }

    public void saveNet() throws IOException {
        ModelSerializer.writeModel(network.getModel(), new File(fullPath + FILE_ENDING), true);
    }

    /**
     * plays games against itself and fills a SampleBuffer for training
     *
     * @param size amount of moves played in total over all games
     * @param moveDepth how often the MCTS-Loop is run for each move
     */
    public void selfPlayGames(int size, int moveDepth, SampleBuffer buffer) throws IOException {
        try {
            for (int i = buffer.getPtr(); i < size; i++) {
                i = bot.selfPlayGame(moveDepth, i, size, buffer);
                bot.resetCore();
                bot.setPos(PositionEncoder.Position.StartingPosition());
            }
        } catch (Exception e) {
            System.err.println("crashed during self-play buffer progress was saved\n error: " + e);
            buffer.writeSamples();
        }
        System.out.println("Finished self play");
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
            SampleBuffer.TrainingSample s = buffer.sample();

            try (INDArray tensorSlice = Nd4j.create(s.tensor, new int[]{PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE}, 'c');
                 INDArray piRow     = Nd4j.create(s.pi);
                 INDArray zRow      = Nd4j.create(new float[]{s.z})) {

                inputs.putSlice(i, tensorSlice);
                piTargets.putRow(i, piRow);
                zTargets.putRow(i, zRow);
            }
        }

        network.getModel().fit(new MultiDataSet(
                new INDArray[] {inputs},
                new INDArray[] {piTargets, zTargets}
        ));

        inputs.close();
        piTargets.close();
        zTargets.close();

        double scores = network.getModel().score();
        System.out.printf("policy loss: %.4f\n", scores);
    }

    /**
     *
     * Trains the AI-model after playing selfPlayGames
     *
     * @param batchSize how big the batch is that the training is used on
     * @param sampleBufferSize amount of positions in the SampleBuffer from the self-play
     * @param n how many MCTS evals per move
     * @param epoch how many times the net will be trained (uses the same sample buffer)
     * @param checkPoint how many batches have to be played till a checkpoint is saved <p></p>
     *                   - checkpoints are set as filename_n_checkPoint.zip
     */
    public void train(int batchSize, int sampleBufferSize, int n, int epoch, int checkPoint, boolean saveBuffer) throws IOException {
        SampleBuffer buffer = new SampleBuffer(sampleBufferSize, fileName + "_buffer");
        train(batchSize, sampleBufferSize, n, epoch, checkPoint, buffer, saveBuffer);
    }

    /**
     * Trains the AI-model with a given SampleBuffer that is then expanded
     *
     * @param batchSize how big the batch is that the training is used on
     * @param sampleBufferSize how big the sample buffer should be in the end
     * @param buffer the buffer used to train with
     * @param saveBuffer should the buffer be saved after filling it
     * @param epoch how many times the net will be trained (uses the same sample buffer)
     * @param checkPoint how many batches have to be played till a checkpoint is saved <p></p>
     *                   - checkpoints are set as filename_n_checkPoint.zip
     */
    public void train(int batchSize, int sampleBufferSize, int n, int epoch, int checkPoint, SampleBuffer buffer, boolean saveBuffer) throws IOException {
        selfPlayGames(sampleBufferSize, n, buffer);
        if(saveBuffer)
            buffer.writeSamples();

        train(batchSize, buffer, epoch, checkPoint);
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
        try {
            for (int i = 1; i < epoch + 1; i++) {
                trainFromBuffer(batchSize, buffer);
                if (i % checkPoint == 0) {
                    saveCheckPoint();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("crashed during training, buffer progress and network were saved");
            buffer.writeSamples();
            saveNet();
        }
    }

    public void diagnose(){
        // After model.init(), before any training
        INDArray testInput = Nd4j.create(PositionEncoder.encodeFlat(PositionEncoder.Position.StartingPosition()), new int[]{1, PositionEncoder.PLANES, 8, 8});
        Map<String, INDArray> acts = network.getModel().feedForward(testInput, false);

        for (String key : new String[]{
                "stem-act", "rb-out-0", "rb-out-4", "rb-out-9", "rb-out-19",
                "pol-act", "val-act"
        }) {
            INDArray a = acts.get(key);
            System.out.printf("%-20s  mean=%.4e  std=%.4e  max=%.4e%n",
                    key, a.meanNumber().doubleValue(),
                    a.stdNumber().doubleValue(),
                    a.maxNumber().doubleValue());
        }
    }


    public static void main(String[] args) throws IOException {
//        ChessBot randBot =  new ChessBot(new Evaluator.RandomEvaluator());
//        SampleBuffer randBuff = new SampleBuffer(5000, "RandBuff");
//        for (int i = 0; i < randBuff.length; i++) {
//            i = randBot.selfPlayGame(300, i, randBuff.length, randBuff);
//            randBot.setPos(PositionEncoder.Position.StartingPosition());
//        }
//        randBuff.writeSamples();


//       second stage training with model
        Train train = new Train("D400_10_RES_BLOCKS",  true, true);
        //train.diagnose();
        SampleBuffer s = new SampleBuffer( 10000, "02_05_2026_full_terminal");
        System.out.println(s.length);
        if(s.length == 0){
            return;
        }
        train.train(32,  s.length, 400, 12000, 4001, s, true);
        train.saveNet();
        train.bot.selfPlayGame(400);
    }
}
