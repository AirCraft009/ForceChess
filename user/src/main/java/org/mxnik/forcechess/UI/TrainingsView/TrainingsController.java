package org.mxnik.forcechess.UI.TrainingsView;

import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.mxnik.forcechess.General.FileLocations;
import org.mxnik.forcechess.Training.Train;
import org.mxnik.forcechess.bot.BatchChessBot;
import org.mxnik.forcechess.network.AlphaNet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrainingsController {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Train trainer;
    private TrainingsView window;

    public void startTraining(
            String modelName,
            String outputName,
            int batchSize,
            int iterations,
            int playDepth
    ) {

        window = new TrainingsView(this);
        window.show();

        running.set(true);

        executor.submit(() -> {
            try {
                window.setStatus("Loading model...");

                String modelPath =
                        FileLocations.NETWORK_LOCATIONS + "/" + modelName;

                AlphaNet net = new AlphaNet(
                        ModelSerializer.restoreComputationGraph(modelPath)
                );

                BatchChessBot bot =
                        new BatchChessBot(net, playDepth, 1F);

                trainer = new Train(
                        bot,
                        outputName.isBlank() ? "BLANK_BOT" : outputName
                );

                window.setStatus("Training started...");

                UIServer uiServer = UIServer.getInstance();
                StatsStorage statsStorage = new InMemoryStatsStorage();
                uiServer.attach(statsStorage);
                window.openDashboard.setDisable(false);

                trainer.train(
                        batchSize,
                        iterations,
                        playDepth,
                        iterations,
                        1000,
                        true
                );

                window.setStatus("Training finished.");

            } catch (Exception e) {
                window.setStatus("Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                running.set(false);
            }
        });
    }

    public void stopTraining() {
        window.setStatus("Stopping...");

        running.set(false);
        trainer.requestStop();

        executor.shutdownNow();

        window.setStatus("Stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }
}