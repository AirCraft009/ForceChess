package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.bot.BatchEvaluator;
import org.mxnik.forcechess.bot.ChessBot;
import org.mxnik.forcechess.bot.Evaluator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ChessScene extends Stage {
    private final String sourcedir = System.getProperty("user.dir") + "/user/src/main/resources/org/mxnik/forcechess/";
    private final String pathToImages = System.getProperty("user.dir") + "/user/src/main/resources/org/mxnik/forcechess/pieces-basic-png/";
    Group root;
    Constants constants;
    private ChessController controller;
    Group backgroundLayer = new Group();
    private Group pieceLayer = new Group();
    private Group interactionLayer = new Group();
    ImageView winView;

    private Image[] images;

    ChessScene(int sideLen) throws CloneNotSupportedException {
        setBounds();
        basicInit(sideLen);
        generateImages();

        getScene().widthProperty().addListener((_, number, t1) -> controller.resize());
        getScene().heightProperty().addListener((_, number, t1) -> controller.resize());

        try {
            this.controller = new ChessController(this, "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
            this.controller.setPlayers(controller, new ChessBot(new Evaluator.StubEvaluator(), 20));
            //this.controller = new ChessController(this, "rnbqkbnrr/ppppppppp/9/9/9/9/9/PPPPPPPPP/RNBQKBNRR w 0 0 0 9");
        }catch (CloneNotSupportedException e){
            throw new CloneNotSupportedException("Error in the chess controller - an invalid clone arose.\nThis is undefined behaviour and should not occur for any reason");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        drawBoard();
        root.getChildren().addAll(backgroundLayer, pieceLayer, interactionLayer);
        this.setOnCloseRequest(e ->
                {
                    cleanUp();
                    Platform.exit();
                    System.exit(0);
                }
        );

        this.controller.start();
    }

    /**
     * sets the x,y & width, height properties
     */
    public void setBounds(){
        setX(Constants.bounds.getMinX());
        setY(Constants.bounds.getMinY());
        setWidth(Constants.bounds.getWidth());
        setHeight(Constants.bounds.getHeight());
    }

    /**
     * initializes root, scene and generates constants for the screen dimensions
     * @param sideLen used to generate screen dimensions
     */
    public void basicInit(int sideLen){
        root = new Group();
        Scene scene = new Scene(root, 500, 500, Color.GREY);
        setTitle("Chess");
        setScene(scene);
        show();
        constants = new Constants(sideLen, scene);
    }

    /**
     * Generate the images beforehand - for better efficiency
     */
    private void generateImages(){
        String[] imagePaths = new String[]{
                pathToImages + "white-pawn.png",
                pathToImages + "black-pawn.png",
                pathToImages + "white-knight.png",
                pathToImages + "black-knight.png",
                pathToImages + "white-bishop.png",
                pathToImages + "black-bishop.png",
                pathToImages + "white-rook.png",
                pathToImages + "black-rook.png",
                pathToImages + "white-queen.png",
                pathToImages + "black-queen.png",
                pathToImages + "white-king.png",
                pathToImages + "black-king.png"
        };
        images = new Image[imagePaths.length];

        for(int i = 0; i < imagePaths.length; i++) {
            try {
                images[i] = new Image(new FileInputStream(imagePaths[i]));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * cleans up the controller
     */
    public void cleanUp(){
        controller.cleanUp();
    }


    // draw Helpers

    /**
     * draws the board BackGround
     */
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
                square.setLayoutY(constants.HeightStart + i * size);


                panes[index] = square;

                //  Click Layer W
                ChessButton button = new ChessButton("", index);
                button.addEventHandler(Event.ANY, controller);

                button.setPrefSize(size, size);
                button.setMinSize(size, size);
                button.setMaxSize(size, size);

                button.setLayoutX(constants.WidthStart + j * size);
                button.setLayoutY(constants.HeightStart + i * size);

                // IMPORTANT -fx-background-color: transparent;
                button.setStyle("-fx-background-color: transparent");

                buttons[index] = button;
            }
        }
        backgroundLayer.getChildren().addAll(panes);
        interactionLayer.getChildren().addAll(buttons);
    }

    /**
     * resets both highlights and pieces
     */
    public void resetBoard(){
        clearPieces();
        clearHighlights();
    }

    /**
     * clears all highlights
     */
    public void clearHighlights(){
        for (int i = 0; i < backgroundLayer.getChildren().size(); i++) {
            ChessBackgroundPane bp = (ChessBackgroundPane) backgroundLayer.getChildren().get(i);
            bp.deactivate();
        }
    }

    /**
     * leaves an empty board (visually)
     */
    public void clearPieces(){
        pieceLayer.getChildren().clear();
    }

    void clearInteractionLayer(){
        interactionLayer.getChildren().clear();
    }

    /**
     * draws all pieces
     */
    public void drawPieces(Board b){
        clearPieces();

        int sideLen = constants.sideLen;
        int knightC = 0;

        for (int i = 0; i < b.getBoard().length; i++) {
            int x = i % sideLen;
            int y = i / sideLen;



            Piece p = b.getBoard()[i];
            if(p.getColor() && p.getType() == PieceTypes.KNIGHT && i == 18) {
                System.out.println("danger knight danger knight");
            }


            if(p.getColor() && p.getType() == PieceTypes.KNIGHT)
                knightC++;

            if (knightC > 2){
                System.out.println("error check state");
                knightC = 0;
            }



            int colorOffset = (p.getColor()?0:1);
            ImageView imgView = switch (p.getType()){
                case PAWN -> new ImageView(images[colorOffset]);
                case KNIGHT -> new ImageView(images[2+colorOffset]);
                case BISHOP -> new ImageView(images[4+colorOffset]);
                case ROOK -> new ImageView(images[6+colorOffset]);
                case QUEEN -> new ImageView(images[8+colorOffset]);
                case KING -> new ImageView(images[10+colorOffset]);
                case ToPromote, EMPTY, ILLEGAL -> null;
            };
            if(imgView == null){
                continue;
            }

            imgView.setX(x * constants.BlockS + constants.WidthStart);
            imgView.setY((sideLen - 1 - y) * constants.BlockS + constants.HeightStart);
            imgView.setFitHeight(constants.BlockS);
            imgView.setFitWidth(constants.BlockS);

            pieceLayer.getChildren().add(imgView);
        }
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
        pieceLayer.getChildren().addFirst(winView);
    }
}
