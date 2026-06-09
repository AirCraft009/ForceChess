package org.mxnik.forcechess.UI.menu;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.FileHandling.TextFileHandler;
import org.mxnik.forcechess.UI.settings.SavedSettings;

import static org.mxnik.forcechess.General.FileLocations.NETWORK_LOCATIONS;

public abstract class MenuPopup extends Stage {
    protected final Stage primaryStage;
    private Alert botAlert = new Alert(Alert.AlertType.WARNING);

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
        ObservableList<String> botsList = FXCollections.observableArrayList(TextFileHandler.getFolderContents(NETWORK_LOCATIONS));
        bots.setItems(botsList);
        if(botsList.isEmpty()){
            botAlert.setContentText("Can't play Game against bot when none are available");
            botAlert.show();
            this.close();
            return null;
        }
        if(!SavedSettings.savedSettings.defaultBot().isBlank()) {
            bots.getSelectionModel().select(SavedSettings.savedSettings.defaultBot());
        }else {
            bots.getSelectionModel().select(0);
        }

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



    /**
     * @param boardSize the single allowed board size. If {@code boardSize <= 0} it contains all layouts
     * @return a {@code ChoiceBox<String>} containing all {@code param}x{@code param} board layouts (Only possibility for bots)
     */
    protected ChoiceBox<String> boardCB(int boardSize) {
        ChoiceBox<String> board = new ChoiceBox<>();
        FenProperties.load();
        ObservableList<String> items = FXCollections.observableArrayList(FenProperties.fenNames);
        if(boardSize > 0) {
            for (int i = 0; i < items.size(); i++) {
                String correspFen = FenProperties.getFenStr(items.get(i));
                if (correspFen.charAt(correspFen.length() - 1) != Integer.toString(boardSize).charAt(0)) {
                    items.remove(items.get(i));
                    i--;
                }
            }
        }
        board.setItems(items);
        return board;
    }
}
