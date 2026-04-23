package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.UI.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class ChessScene extends Stage {
    private final String sourcedir = System.getProperty("user.dir") + "/src/main/resources/org/mxnik/forcechess/";
    private final String pathToImages = System.getProperty("user.dir") + "/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";
    Group root;
    Constants constants;
    private ChessController controller = null;
    private HashMap<Integer, ImageView> pieceImages = new HashMap<>();
    Group backgroundLayer = new Group();
    private Group pieceLayer = new Group();
    private Group interactionLayer = new Group();
    ImageView winView;

    ChessScene() throws CloneNotSupportedException {
        setX(Constants.bounds.getMinX());
        setY(Constants.bounds.getMinY());
        setWidth(Constants.bounds.getWidth());
        setHeight(Constants.bounds.getHeight());

        root = new Group();
        Scene scene = new Scene(root, 500, 500, Color.GREY);
        setTitle("Chess");
        setScene(scene);
        show();
        constants = new Constants(8, scene);

//        this.controller = new ChessController(this, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        try {
            System.out.println("pre controller");
            this.controller = new ChessController(this, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
            System.out.println("set control");
            this.controller.setPlayers(controller, controller);
            //this.controller = new ChessController(this, "rnbqkbnrr/ppppppppp/9/9/9/9/9/PPPPPPPPP/RNBQKBNRR w 0 0 0 9");
        }catch (CloneNotSupportedException e){
            throw new CloneNotSupportedException("Error in the chess controller - an invalid clone arose.\nThis is undefined behaviour and should not occur for any reason");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        drawBoard();
        root.getChildren().addAll(backgroundLayer, pieceLayer, interactionLayer);
        this.controller.start();
        this.setOnCloseRequest(e ->
                {
                    cleanUp();
                    Platform.exit();
                    System.exit(0);
                }
        );
    }

    public void drawBoard() {
        int sideLen = constants.sideLen;
        int size = constants.BlockS;
        int index;
        ChessBackgroundPane[] panes =  new ChessBackgroundPane[Board.size];
        ChessButton[] buttons =  new ChessButton[Board.size];


        for (int i = 0; i < sideLen; i++) {
            for (int j = 0; j < sideLen; j++) {
                int logCol = sideLen - 1 - i;
                index = logCol * sideLen + j;;
                //  Background
                ChessBackgroundPane square;

                if ((i + j) % 2 == 0) {
                    square = new ChessBackgroundPane(size, size, Color.WHITE, Color.WHEAT, index);

                } else {
                    square = new ChessBackgroundPane(size, size, Color.DARKBLUE, Color.LIGHTBLUE, index);
                }

                square.setLayoutX(constants.WidthStart + j * size);
                square.setLayoutY(i * size);


                panes[index] = square;

                //  Click Layer W
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

    public void resetBoard(){
        clearPieces();
        clearHighlights();
    }


    public void clearHighlights(){
        for (int i = 0; i < backgroundLayer.getChildren().size(); i++) {
            ChessBackgroundPane bp = (ChessBackgroundPane) backgroundLayer.getChildren().get(i);
            bp.deactivate();
        }
    }

    public void clearPieces(){
        pieceLayer.getChildren().clear();
    }

    public void drawPieces(Piece[] pieces){
        int sideLen = constants.sideLen;

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

            pieceImages.put(i, imageView);
            pieceLayer.getChildren().add(imageView);
        }
    }

    public void cleanUp(){
        controller.cleanUp();
    }

    public void showWinImage(){
        String imageP = sourcedir + "img.png";
        Image image;
        try {
            image = new Image(new FileInputStream(imageP));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        winView = new ImageView(image);
        winView.setFitHeight(constants.bounds.getHeight());
        winView.setFitWidth(constants.bounds.getWidth());
        pieceLayer.getChildren().add(winView);
    }

    public void movePiece(int from, int to){
        int sideLen = constants.sideLen;
        int size = constants.BlockS;

        ImageView i = pieceImages.remove(from);
        pieceImages.remove(to);
        if(i == null){
            return;
        }
        int x = (to % sideLen)* size + constants.WidthStart;
        int y = (sideLen - to / sideLen - 1) * size ;
        i.setX(x);
        i.setY(y);
        pieceImages.put(to, i);
    }

    public static void main(String[] args) {

    }
}
