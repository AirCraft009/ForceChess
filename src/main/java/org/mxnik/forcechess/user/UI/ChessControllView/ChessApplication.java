package org.mxnik.forcechess.user.UI.ChessControllView;

import javafx.application.Application;
import javafx.stage.Stage;

public class ChessApplication extends Application {
    private final String pathToImages = System.getProperty("user.dir") + "/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";

    @Override
    public void start(Stage primaryStage) throws CloneNotSupportedException {
        new ChessScene();
    }

    public static void main(String[] args) {
        launch(ChessApplication.class);
    }
}
