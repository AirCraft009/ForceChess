package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Application;
import javafx.stage.Stage;

public class ChessApplication extends Application {
    private final String pathToImages = System.getProperty("user.dir") + "user/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";

    @Override
    public void start(Stage primaryStage) throws CloneNotSupportedException {
        new ChessScene(8, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    public static void main(String[] args) {
        launch(ChessApplication.class);
    }
}
