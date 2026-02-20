package org.mxnik.forcechess.UI;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class ChessScene extends Application {
    Constants constants;

    @Override
    public void start(Stage primaryStage) {
        constants = new Constants(8);

        Group root = new Group();
        Scene scene = new Scene(root, 500, 500, Color.GREY);

        primaryStage.setX(constants.bounds.getMinX());
        primaryStage.setY(constants.bounds.getMinY());
        primaryStage.setWidth(constants.bounds.getWidth());
        primaryStage.setHeight(constants.bounds.getHeight());

        drawBoard(8, root);

        primaryStage.setTitle("java-buddy.blogspot.com");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void drawBoard(int sideLen, Group root){
        Rectangle[] rectangles = new Rectangle[sideLen * sideLen];

        int currPosX = constants.WidthStart;
        int size = constants.BlockS;
        int point = 0;
        int sideLengthScreen = constants.BoardSize;

        for (int i = 0; i < sideLen; i++) {
            for (int j = 0; j < sideLen; j++) {
                point = i + j*sideLen;
                Rectangle r1 = new Rectangle(currPosX + size * j, size * i,  size, size);
                if((j + i) % 2 == 0){
                    r1.setFill(Color.BLACK);
                }else {
                    r1.setFill(Color.WHITE);
                }

                rectangles[point] = r1;
            }
        }

        root.getChildren().addAll(rectangles);
    }

    public static void main(String[] args) {
        Application.launch(ChessScene.class);
    }

}
