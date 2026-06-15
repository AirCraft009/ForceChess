package org.mxnik.forcechess.UI.menu;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.DragEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.mxnik.forcechess.UI.ChessControllView.ChessView;
import org.mxnik.forcechess.UI.Constants;

import java.io.IOException;

public class MenuScene {
    VBox root;
    Stage stage;
    Constants constants;
    MenuController controller;

    Button pvp, pvb, bvb, cBoard, settings;

    /**
     * @param stage the primary stage used in the program
     */
    public MenuScene(Stage stage) {
        this.stage = stage;
        setBounds(stage);
        basicInit(stage);

        stage.getScene().widthProperty().addListener((_, number, t1) -> controller.resize());
        stage.getScene().heightProperty().addListener((_, number, t1) -> controller.resize());

        this.controller = new MenuController(this);
        root.addEventHandler(DragEvent.ANY, controller);

        drawMenu();
        stage.setOnCloseRequest(e ->
                {
                    Platform.exit();
                    System.exit(0);
                }
        );
    }

    /**
     * sets the {@code x}, {@code y} and {@code width}, {@code height} properties
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
        try {
            var icon = ChessView.getImageFromRessource(ChessView.sourcedir + "icon.png");
            if(icon != null) {
                stage.getIcons().add(icon);
            }else {
                System.err.println("Error when instantiating the inputStream");
            }
        } catch (IOException e) {
            // Ignore unloaded icon
            System.err.println("Icon couldn't be loaded");
        }
        stage.show();
        constants = new Constants(-1, scene);
    }

    /**
     * Create the layout of the menu scene
     */
    public void drawMenu(){
        root.setSpacing(stage.getScene().getHeight() /24);
        root.setAlignment(Pos.CENTER);

        pvp = createButton("Player vs Player");
        pvb = createButton("Player vs Bot");
        bvb = createButton("Bot vs Bot");
        cBoard = createButton("Create Board");
        settings = createButton("Settings");

        root.getChildren().addAll(pvp, pvb, bvb, cBoard, settings);
    }

    /**
     * @param text The text the button should display
     * @return the formatted button
     */
    private Button createButton(String text){
        double sceneW = stage.getScene().getWidth();
        double sceneH = stage.getScene().getHeight();
        Button b = new Button(text);
        b.addEventHandler(Event.ANY, controller);
        b.setPrefSize(sceneW /5, sceneH /12);
        Constants.defaultStyleButton(b, stage.getScene(), true);
        return b;
    }

    public Scene getScene(){
        return stage.getScene();
    }
}
