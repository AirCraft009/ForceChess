package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Application;
import javafx.stage.Stage;
import org.mxnik.forcechess.UI.menu.MenuScene;
import org.mxnik.forcechess.UI.settings.SavedSettings;

public class ChessApplication extends Application {
    @Override
    public void start(Stage primaryStage) {
        SavedSettings.loadSavedSettings();
        new MenuScene(primaryStage);
    }

    public static void main(String[] args) {
        launch(ChessApplication.class);
    }
}
