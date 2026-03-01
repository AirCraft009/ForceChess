package org.mxnik.forcechess.ChesGame.UI.ChessControllView;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.stage.Stage;
import org.mxnik.forcechess.Util.Constants;

public class ChessApplication extends Application {
    private final String pathToImages = System.getProperty("user.dir") + "/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";
    Group root;
    Constants constants;

    @Override
    public void start(Stage primaryStage) {
        new ChessScene();
    }

    public static void main(String[] args) {
        launch(ChessApplication.class);
    }
}
