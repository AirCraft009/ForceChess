package org.mxnik.forcechess.UI.ChessCreation;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.UI.ChessControllView.ChessBackgroundPane;
import org.mxnik.forcechess.UI.ChessControllView.ChessButton;
import org.mxnik.forcechess.UI.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class BoardCreationView {
    public final Stage stage;

    private final String sourcedir = System.getProperty("user.dir") + "/src/main/resources/org/mxnik/forcechess/";
    private final String pathToImages = sourcedir + "pieces-basic-png/";
    BorderPane borderPane;
    Constants constants;
    private BoardCreationController controller;
    Group center = new Group();
    Group backgroundLayer = new Group();
    private Group pieceLayer = new Group();
    private Group interactionLayer = new Group();
    CheckBox white;
    ListView<Group> listView;

    ImageView winView;

    private Image[] images;

    public BoardCreationView(Stage stage, String fen, int sideLen) {
        this.stage = stage;

        setBounds();
        basicInit(sideLen);
        generateImages();

        stage.getScene().widthProperty().addListener((_, number, t1) -> controller.resize());
        stage.getScene().heightProperty().addListener((_, number, t1) -> controller.resize());

        this.controller = new BoardCreationController(this, stage, fen);

        drawListView(true);
        drawBoard();
        center.getChildren().addAll(backgroundLayer, pieceLayer, interactionLayer);
        stage.setOnCloseRequest(e ->
                {
                    Platform.exit();
                    System.exit(0);
                }
        );
    }

    /**
     * sets the x,y & width, height properties
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
        borderPane = new BorderPane();
        borderPane.setCenter(center);
        Scene scene = new Scene(borderPane, 500, 500, Color.GREY);
        stage.setTitle("Board Creation");
        stage.setScene(scene);
        stage.show();
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

    public void drawListView(boolean w){
        if(borderPane.getRight() == null){
            HBox hBox = new HBox();
            white = new CheckBox("White");
            listView = new ListView<Group>();
            listView.getSelectionModel().selectedItemProperty().addListener(controller);
        }
        Group[] pieces = new Group[6];
        String colorPrefix = (w) ? "white-" : "black-";
        String[] arr = {colorPrefix + "pawn.png",
                colorPrefix + "knight.png",
                colorPrefix + "bishop.png",
                colorPrefix + "rook.png",
                colorPrefix + "queen.png",
                colorPrefix + "king.png"};
        for(int i = 0; i < arr.length; i++) {
            int size = constants.BlockS;
            ChessBackgroundPane bg = new ChessBackgroundPane(size, size, (i%2==0)?Color.WHITE:Color.DARKBLUE, (i%2==0)?Color.WHEAT:Color.LIGHTBLUE, i);

            ImageView piece = new ImageView(images[2*i+(w?0:1)]);
            piece.setFitHeight(size);
            piece.setFitWidth(size);

            ChessButton button = new ChessButton("", i);
            button.addEventHandler(Event.ANY, controller);

            button.setPrefSize(size, size);
            button.setMinSize(size, size);
            button.setMaxSize(size, size);

            // IMPORTANT -fx-background-color: transparent;
            button.setStyle("-fx-background-color: transparent");

            pieces[i].getChildren().addAll(bg, piece, button);
        }
        ObservableList<Group> observableList = FXCollections.observableList(Arrays.asList(pieces));
        listView.setItems(observableList);
    }


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
    public void drawPieces(Piece[] pieces){
        clearPieces();

        int sideLen = constants.sideLen;

        for (int i = 0; i < pieces.length; i++) {
            int x = i % sideLen;
            int y = i / sideLen;


            ImageView imgView = getImageView(pieces, i);
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

    @Nullable
    private ImageView getImageView(Piece[] pieces, int i) {
        Piece p = pieces[i];


        int colorOffset = (p.getColor()?0:1);
        return switch (p.getType()){
            case PAWN -> new ImageView(images[colorOffset]);
            case KNIGHT -> new ImageView(images[2+colorOffset]);
            case BISHOP -> new ImageView(images[4+colorOffset]);
            case ROOK -> new ImageView(images[6+colorOffset]);
            case QUEEN -> new ImageView(images[8+colorOffset]);
            case KING -> new ImageView(images[10+colorOffset]);
            case ToPromote, EMPTY, ILLEGAL -> null;
        };
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
