package org.mxnik.forcechess.UI.menu;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.mxnik.forcechess.UI.Constants;

public class MenuScene {
    VBox root;
    Stage stage;
    Constants constants;
    MenuController controller;

    Button pvp, pvb, bvb, train, cBoard, settings;

    /**
     * @param stage the primary stage used in the program
     */
    public MenuScene(Stage stage) {
        this.stage = stage;
        setBounds(stage);
        basicInit(stage);

        //TODO remove listeners when not needed
        stage.getScene().widthProperty().addListener((_, number, t1) -> controller.resize());
        stage.getScene().heightProperty().addListener((_, number, t1) -> controller.resize());

        this.controller = new MenuController(this);

        drawMenu();
        stage.setOnCloseRequest(e ->
                {
                    Platform.exit();
                    System.exit(0);
                }
        );
    }

    /**
     * sets the {@code x}, {@code y} & {@code width}, {@code height} properties
     */
    public void setBounds(Stage stage) {
        stage.setX(Constants.bounds.getMinX());
        stage.setY(Constants.bounds.getMinY());
        stage.setWidth(Constants.bounds.getWidth());
        stage.setHeight(Constants.bounds.getHeight());
    }

    /**
     * initializes root, scene and generates constants for the screen dimensions
     * @param stage used to generate screen dimensions
     */
    public void basicInit(Stage stage){
        root = new VBox();
        Scene scene = new Scene(root, 500, 500, Color.GREY);
        stage.setTitle("Chess");
        stage.setScene(scene);
        stage.show();
        constants = new Constants(-1, scene);
    }

    /**
     * Create the layout of the menu scene
     */
    public void drawMenu(){
        root.setSpacing(20);
        root.setAlignment(Pos.CENTER);

        pvp = createButton("Player vs Player");
        pvb = createButton("Player vs Bot");
        bvb = createButton("Bot vs Bot");
        train = createButton("Train Bots");
        cBoard = createButton("Create Board");
        settings = createButton("Settings");

        root.getChildren().addAll(pvp, pvb, bvb, train, cBoard, settings);
    }

    /**
     * @param text The text the button should display
     * @return the formatted button
     */
    private Button createButton(String text){
        Button button = new Button(text);
        button.addEventHandler(Event.ANY, controller);
        button.setFont(Font.font("Verdana", 24));
        return button;
    }

    public Scene getScene(){
        return stage.getScene();
    }
}
