package org.mxnik.forcechess.UI;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ChessScene extends Application {
    private final String pathToImages = System.getProperty("user.dir") + "/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";
    Group root;
    Constants constants;

    @Override
    public void start(Stage primaryStage) {
        constants = new Constants(8);

        root = new Group();
        Scene scene = new Scene(root, 500, 500, Color.GREY);

        primaryStage.setX(constants.bounds.getMinX());
        primaryStage.setY(constants.bounds.getMinY());
        primaryStage.setWidth(constants.bounds.getWidth());
        primaryStage.setHeight(constants.bounds.getHeight());

        primaryStage.setTitle("java-buddy.blogspot.com");
        primaryStage.setScene(scene);
        primaryStage.show();

        constants = new Constants(8, scene);
        drawBoard(8);

        //TODO just a test method, needs to be removed.
        Piece[] arr = new FenNotation("R1BQKBNR/PPPPPPPP/2N5/8/8/8/pppppppp/rnbqkbnr . . . . 8").readFenBoard();
        drawPieces(arr, 8);
    }

    public void drawBoard(int sideLen){
        Rectangle[] rectangles = new Rectangle[sideLen * sideLen];

        int currPosX = constants.WidthStart;
        int size = constants.BlockS;
        int point;

        for (int i = 0; i < sideLen; i++) {
            for (int j = 0; j < sideLen; j++) {
                point = i + j*sideLen;
                Rectangle r1 = new Rectangle(currPosX + size * j, size * i,  size, size);
                if((j + i) % 2 == 0){
                    r1.setFill(constants.WhiteColor);
                }else {
                    r1.setFill(constants.DarkColor);
                }
                rectangles[point] = r1;
            }
        }

        root.getChildren().addAll(rectangles);
    }

    public void drawPieces(Piece[] pieces, int sideLen){
        for (int i = 0; i < pieces.length; i++) {
            int x = i % sideLen;
            int y = i / sideLen;

            Piece p = pieces[i];
            String imageP;
            switch (p.getType()){
                case PAWN -> imageP = pathToImages + (p.getColor()?"white-":"black-") + "pawn.png";
                case KNIGHT -> imageP = pathToImages + (p.getColor()?"white-":"black-") + "knight.png";
                case BISHOP -> imageP = pathToImages + (p.getColor()?"white-":"black-") + "bishop.png";
                case ROOK -> imageP = pathToImages + (p.getColor()?"white-":"black-") + "rook.png";
                case QUEEN -> imageP = pathToImages + (p.getColor()?"white-":"black-") + "queen.png";
                case KING -> imageP = pathToImages + (p.getColor()?"white-":"black-") + "king.png";
                default -> {continue;}
            }

            Image image;
            try {
                image = new Image(new FileInputStream(imageP));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            ImageView imageView = new ImageView(image);
            imageView.setX(x * constants.BlockS + constants.WidthStart);
            imageView.setY(y * constants.BlockS);
            imageView.setFitHeight(constants.BlockS);
            imageView.setFitWidth(constants.BlockS);

            root.getChildren().add(imageView);
        }
    }

    public static void main(String[] args) {
        Application.launch(ChessScene.class);
    }

}
