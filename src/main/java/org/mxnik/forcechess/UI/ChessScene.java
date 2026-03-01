package org.mxnik.forcechess.UI;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.Util.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class ChessScene extends Stage {
    private final String pathToImages = System.getProperty("user.dir") + "/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";
    Group root;
    Constants constants;
    private final ChessController controller;
    Group backgroundLayer = new Group();
    private Group pieceLayer = new Group();
    private Group interactionLayer = new Group();

    ChessScene(){
        constants = new Constants(8);

        root = new Group();
        Scene scene = new Scene(root, 500, 500, Color.GREY);

        setX(constants.bounds.getMinX());
        setY(constants.bounds.getMinY());
        setWidth(constants.bounds.getWidth());
        setHeight(constants.bounds.getHeight());


        constants = new Constants(8, scene);
        setTitle("Chess");
        setScene(scene);
        this.controller = new ChessController(this, "rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        drawBoard(8);
        root.getChildren().addAll(backgroundLayer, pieceLayer, interactionLayer);
        show();
    }

    public void drawBoard(int sideLen) {
        int size = constants.BlockS;
        int index;
        ChessBackgroundPane[] panes =  new ChessBackgroundPane[Board.size];
        ChessButton[] buttons =  new ChessButton[Board.size];


        for (int i = 0; i < sideLen; i++) {
            for (int j = 0; j < sideLen; j++) {
                int logCol = sideLen - 1 - i;
                index = logCol * sideLen + j;;
                // --- Background ---
                ChessBackgroundPane square = new ChessBackgroundPane(size, size, index);

                if ((i + j) % 2 == 0) {
                    square.setFill(Color.WHITE);
                } else {
                    square.setFill(Color.DARKBLUE);
                }

                square.setLayoutX(constants.WidthStart + j * size);
                square.setLayoutY(i * size);


                panes[index] = square;

                // --- Click Layer ---
                ChessButton button = new ChessButton("", index);
                button.addEventHandler(ActionEvent.ACTION, controller);

                button.setPrefSize(size, size);
                button.setMinSize(size, size);
                button.setMaxSize(size, size);

                button.setLayoutX(constants.WidthStart + j * size);
                button.setLayoutY(i * size);

                // IMPORTANT -fx-background-color: transparent;
                button.setStyle("-fx-background-color: transparent");

                buttons[index] = button;
            }
        }
        backgroundLayer.getChildren().addAll(panes);
        interactionLayer.getChildren().addAll(buttons);
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

            //System.out.println(x + " : " + y + "\ni: " + i + "\n piece: " + pieces[i] + "\nimage: " + imageP);

            Image image;
            try {
                image = new Image(new FileInputStream(imageP));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            ImageView imageView = new ImageView(image);
            imageView.setX(x * constants.BlockS + constants.WidthStart);
            imageView.setY((sideLen - 1 - y) * constants.BlockS);
            imageView.setFitHeight(constants.BlockS );
            imageView.setFitWidth(constants.BlockS);

            pieceLayer.getChildren().add(imageView);
        }
    }

    public static void main(String[] args) {

    }
}
