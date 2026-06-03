package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Application;
import javafx.stage.Stage;
import org.mxnik.forcechess.UI.menu.MenuScene;
import org.mxnik.forcechess.UI.settings.SavedSettings;

public class ChessApplication extends Application {
    private final String pathToImages = System.getProperty("user.dir") + "user/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";

    @Override
    public void start(Stage primaryStage) throws CloneNotSupportedException {
        SavedSettings.loadSavedSettings();
        new MenuScene(primaryStage);
    }

    public static void main(String[] args) {
        launch(ChessApplication.class);
    }
}
