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
    protected static final String DIRECTORY = System.getProperty("user.dir");

    protected MenuPopup() {
        super();
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);
        setMinWidth(800);
        setMinHeight(600);
    }

    protected ChoiceBox<String> getBotsList() {
        ChoiceBox<String> bots = new ChoiceBox<>();
        ObservableList<String> botsList = FXCollections.observableArrayList(TextFileHandler.getFolderContents(DIRECTORY + "/boardsNBots/bots/networks"));
        bots.setItems(botsList);
        return bots;
    }
    protected Button getButton(String text) {
        Button button = new Button(text);
        return button;
    }
}
