package org.mxnik.forcechess.UI.menu;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.mxnik.forcechess.FileHandling.TextFileHandler;

public abstract class MenuPopup extends Stage {
    protected final Stage primaryStage;
    protected static final String DIRECTORY = System.getProperty("user.dir");

    /**
     * The base for any popup-window in the menu scene
     * @param primaryStage the primary stage used in the program
     */
    protected MenuPopup(Stage primaryStage) {
        super();
        this.primaryStage = primaryStage;
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);
        setMinWidth(400);
        setMinHeight(700);
    }

    /**
     * @return A {@code ChoiceBox<String>} with all chess-bots saved on the current device.
     */
    protected ChoiceBox<String> getBotsList() {
        ChoiceBox<String> bots = new ChoiceBox<>();
        ObservableList<String> botsList = FXCollections.observableArrayList(TextFileHandler.getFolderContents(DIRECTORY + "/../boardsNBots/bots/networks"));
        bots.setItems(botsList);
        return bots;
    }

    /**
     * @param text the text the button will display
     * @return the correctly formatted {@code Button}
     */
    protected Button getButton(String text) {
        Button button = new Button(text);
        return button;
    }
}
