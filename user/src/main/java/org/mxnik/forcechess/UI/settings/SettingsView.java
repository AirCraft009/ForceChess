package org.mxnik.forcechess.UI.settings;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.mxnik.forcechess.FileHandling.TextFileHandler;
import org.mxnik.forcechess.UI.Constants;

import static org.mxnik.forcechess.General.FileLocations.NETWORK_LOCATIONS;

public class SettingsView {
    SettingsController controller;
    Stage stage;
    Constants constants;
    GridPane root;

    TextField savePathTF;
    ColorPicker colorLight, colorDark;
    ColorPicker highlightLight, highlightDark;
    ColorPicker movedLight, movedDark;
    ChoiceBox<String> defaultBot;
    Button closeButton, resetButton, defaultButton;

    public SettingsView(Stage primaryStage, boolean loadSettings) {
        stage = primaryStage;
        setBounds();
        basicInit(8);
        controller = new SettingsController(this);
        if(loadSettings) {
            loadSettings();
        }

        stage.getScene().widthProperty().addListener((_, number, t1) -> controller.resize());
        stage.getScene().heightProperty().addListener((_, number, t1) -> controller.resize());

        Label savePathL = createFormattedLabel("AI Path: ");
        root.add(savePathL, 0, 0);
        savePathTF = createFormattedTF("C:\\Users\\ME\\ForceChess\\boardsNBots", "Where your AI-Models will be");
        savePathTF.setText(SavedSettings.savedSettings.savePath());
        savePathTF.textProperty().addListener(controller);
        root.add(savePathTF, 1, 0, 3, 1);


        Label lightSquareL = new Label("Light Square Color: ");
        root.add(lightSquareL, 0, 1);
        colorLight = createColorPicker(SavedSettings.savedSettings.lightSquare(), controller);
        root.add(colorLight, 1, 1);

        Label darkSquareL = new Label("Dark Square Color: ");
        root.add(darkSquareL, 2, 1);
        colorDark = createColorPicker(SavedSettings.savedSettings.darkSquare(), controller);
        root.add(colorDark, 3, 1);

        Label lightHighlightL = new Label("Light Square Highlight: ");
        root.add(lightHighlightL, 0, 2);
        highlightLight = createColorPicker(SavedSettings.savedSettings.lightHighlight(), controller);
        root.add(highlightLight, 1, 2);

        Label darkHighlightL = new Label("Dark Square Highlight: ");
        root.add(darkHighlightL, 2, 2);
        highlightDark = createColorPicker(SavedSettings.savedSettings.darkHighlight(), controller);
        root.add(highlightDark, 3, 2);

        Label lightMovedL = new Label("Light Square Moved: ");
        root.add(lightMovedL, 0, 3);
        movedLight = createColorPicker(SavedSettings.savedSettings.lightMoved(), controller);
        root.add(movedLight, 1, 3);

        Label darkMovedL = new Label("Dark Square Moved: ");
        root.add(darkMovedL, 2, 3);
        movedDark = createColorPicker(SavedSettings.savedSettings.darkMoved(), controller);
        root.add(movedDark, 3, 3);

        Label defaultBotL = createFormattedLabel("Default Bot: ");
        root.add(defaultBotL, 0, 4, 2, 1);
        defaultBot = new ChoiceBox<>();
        ObservableList<String> botsList = FXCollections.observableArrayList(TextFileHandler.getFolderContents(NETWORK_LOCATIONS));
        botsList.add("");
        defaultBot.setItems(botsList);
        defaultBot.getSelectionModel().select(SavedSettings.savedSettings.defaultBot());
        defaultBot.getSelectionModel().selectedItemProperty().addListener(controller);
        root.add(defaultBot, 2, 4, 2, 1);

        closeButton = new Button("Close");
        closeButton.addEventHandler(ActionEvent.ACTION, controller);
        root.add(closeButton, 0, 5);
        resetButton = new Button("Reset");
        resetButton.setVisible(false);
        resetButton.addEventHandler(ActionEvent.ACTION, controller);
        root.add(resetButton, 2, 5);
        defaultButton = new Button("Reset to Default");
        defaultButton.addEventHandler(ActionEvent.ACTION, controller);
        root.add(defaultButton, 3, 5);


        stage.setOnCloseRequest(e ->
                {
                    Platform.exit();
                    System.exit(0);
                }
        );
    }

    /**
     * sets the x,y, width, height properties
     */
    public void setBounds(){
        stage.setX(Constants.bounds.getMinX());
        stage.setY(Constants.bounds.getMinY());
        stage.setWidth(Constants.bounds.getWidth());
        stage.setHeight(Constants.bounds.getHeight());
    }

    /**
     * initializes root, scene and generates constants for the screen dimensions
     * @param sideLen used to generate screen dimensions
     */
    public void basicInit(int sideLen){
        root = new GridPane();
        root.setHgap(30);
        root.setVgap(50);
        root.setPadding(new Insets(50));
        Scene scene = new Scene(root, 500, 500, Color.GREY);
        stage.setTitle("Chess");
        stage.setScene(scene);
        stage.show();
        constants = new Constants(sideLen, scene);
    }

    /**
     * Loads the saved Settings from the properties file
     */
    void loadSettings(){
        SavedSettings.loadSavedSettings();
    }

    //Methods for simpler, shorter UI-Changes
    private Label createFormattedLabel(String text) {
        return new Label(text);
    }
    private TextField createFormattedTF(String promptText, String tooltip) {
        TextField tf = new TextField();
        tf.setPromptText(promptText);
        tf.setTooltip(new Tooltip(tooltip));
        return tf;
    }
    private ColorPicker createColorPicker(Color value, SettingsController controller) {
        ColorPicker c = new ColorPicker();
        c.setStyle("-fx-color-label-visible: false;");
        c.setPrefSize(50, 30);
        c.setValue(value);
        c.valueProperty().addListener(controller);
        return c;
    }
    private RadioButton createRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        return rb;
    }
}
