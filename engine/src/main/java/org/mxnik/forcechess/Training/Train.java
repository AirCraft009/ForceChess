package org.mxnik.forcechess.Training;


import org.bytedeco.javacv.FrameFilter;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.General.ConsoleBar;
import org.mxnik.forcechess.General.FileLocations;
import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.PositionEncoder;
import org.mxnik.forcechess.bot.BatchChessBot;
import org.mxnik.forcechess.bot.ChessBot;
import org.mxnik.forcechess.bot.Evaluator;
import org.mxnik.forcechess.network.AlphaNet;
import org.mxnik.forcechess.network.NetworkConfig;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
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
            System.out.println();
            System.out.println("\tStarting self-play game session: ");
            System.out.println("-".repeat(ConsoleBar.WIDTH + 2));
            for (int i = 0; i + buffer.getPtr() < size; i++) {
                i = bot.selfPlayGame(moveDepth, i, size - buffer.getPtr(), buffer);
                bot.resetCore();
                bot.setPos(PositionEncoder.Position.StartingPosition());
            }
        } catch (Exception e) {
            System.err.println("crashed during self-play buffer progress was saved\n error: " + e);
            buffer.writeSamples();
        }
        System.out.println();
        System.out.println("Finished self play");
    }

    /**
     * fit batchsize samples to the model randomly picked from the sample buffer
     */
    private void trainFromBuffer(int batchSize, SampleBuffer buffer, INDArray inputs, INDArray piTargets, INDArray zTargets){
        // each sample: one position + its pi + its z
        for (int i = 0; i < batchSize; i++) {
            SampleBuffer.TrainingSample s = buffer.getNext();

//            float sum = 0;
//            float max = 0;
//            float count = 0;
//            for (float f : s.pi) {
//                sum += f;
//                count += (f == 0)? 0 : 1;
//                if (f > max) max = f;
//            }
//
//            System.out.println("sum: " + sum);
//            System.out.println("max: " + max);
//            System.out.println("moves: " + count);
//            System.out.println("uniform would be: " + (1.0f / count));

            try (INDArray tensorSlice = Nd4j.create(s.tensor, new int[]{PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE}, 'c');
                 INDArray piRow     = Nd4j.create(s.pi);
                 INDArray zRow      = Nd4j.create(new float[]{s.z})) {

                inputs.putSlice(i, tensorSlice);
                piTargets.putRow(i, piRow);
                zTargets.putRow(i, zRow);
            }
        }
        // scale down zTargets because piTargets are way smaller in comp.
        zTargets.muli(0.2);

        network.getModel().fit(new MultiDataSet(
                new INDArray[] {inputs},
                new INDArray[] {piTargets, zTargets}
        ));


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
        SampleBuffer buffer = new SampleBuffer(sampleBufferSize, fileName, false);
        train(batchSize, sampleBufferSize, n, epoch, checkPoint, buffer, saveBuffer);
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
     * @param sequences how many times the process should be repeated (self-play + training)
     */
    public void train(int batchSize, int sampleBufferSize, int n, int epoch, int checkPoint, int sequences, boolean saveBuffer) throws IOException {
        SampleBuffer buffer = new SampleBuffer(sampleBufferSize * sequences, fileName, false);
        for (int i = 0; i < sequences; i++) {
            train(batchSize, sampleBufferSize, n, epoch, checkPoint, buffer, saveBuffer);
        }

    }

    /**
     *
     * Trains the AI-model after playing selfPlayGames
     *
     * @param batchSize how big the batch is that the training is used on
     * @param n how many MCTS evals per move
     * @param epoch how many times the net will be trained (uses the same sample buffer)
     * @param checkPoint how many batches have to be played till a checkpoint is saved <p></p>
     *                   - checkpoints are set as filename_n_checkPoint.zip
     * @param sequences how many times the process should be repeated (self-play + training)
     */
    public void train(int batchSize, SampleBuffer buffer, int increment, int n, int epoch, int checkPoint, int sequences, boolean saveBuffer) throws IOException {
        System.out.println("Starting sequential training");
        for (int i = 0; i < sequences; i++) {
            System.out.printf("sequence %d out of %d\n", i, sequences);
            train(batchSize, buffer.getPtr()+increment, n, epoch, checkPoint, buffer, saveBuffer);
        }

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
        buffer.shuffel();

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
        // SETUP
        // inputs: [batchSize, PLANES, 8, 8]
        // policy targets: [batchSize, 65536] (the pi distribution)
        // value targets: [batchSize, 1] (the game outcome z)
        // initialize arrays ones to avoid alloc and dealloc
        try (INDArray inputs = Nd4j.zeros(batchSize, PositionEncoder.PLANES, PositionEncoder.SIZE, PositionEncoder.SIZE);
             INDArray piTargets = Nd4j.zeros(batchSize, Move.MOVE_POSSIBILITIES);
             INDArray zTargets = Nd4j.zeros(batchSize, 1)) {

            buffer.shuffel();

            trainFromBuffer(batchSize, buffer, inputs, piTargets, zTargets);
            System.out.println("\tstartLoss: " + network.getModel().score());
            System.out.println("-".repeat(ConsoleBar.WIDTH + 2));
            for (int i = 1; i < epoch + 1; i++) {
                trainFromBuffer(batchSize, buffer, inputs, piTargets, zTargets);
                ConsoleBar.render((double) i / epoch, 2);
                if (checkPoint > 0 && i % checkPoint == 0) {
                    saveCheckPoint();
                }
            }
            System.out.println("\tendLoss: " + network.getModel().score());
            System.out.println("-".repeat(ConsoleBar.WIDTH + 2));
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

    public double getLoss(){
        return network.getModel().score();
    }

    public AlphaNet getNetwork(){
        return network;
    }


    public static void main(String[] args) throws IOException {

//       second stage training with model
        Train train = new Train("Endgame_1_checkPoint",  true, true);
        //train.diagnose();

        System.out.println(Nd4j.getBackend().getClass().getName());



        try{
            // Monitor
            UIServer uiServer = UIServer.getInstance();
            StatsStorage statsStorage = new InMemoryStatsStorage();
            uiServer.attach(statsStorage);
            train.network.getModel().setListeners(new StatsListener(statsStorage, 5));

            for (int i = 2; i < 6; i++) {
                SampleBuffer b = new SampleBuffer("BalancedBuffer"+i);
                train.train(512, b ,290,-1);
                train.saveCheckPoint();
                train.saveNet();
                b = null;
                System.gc();
            }


        }catch (Exception e){
            train.saveNet();
            System.out.println(e);
            System.err.println("encountered exception");
        }
        train.saveNet();
        System.out.println("saved Network");
    }
}
