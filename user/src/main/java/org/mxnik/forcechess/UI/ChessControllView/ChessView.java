package org.mxnik.forcechess.UI.ChessControllView;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.deeplearning4j.util.ModelSerializer;
import org.jetbrains.annotations.Nullable;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.General.FileLocations;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.bot.BatchChessBot;
import org.mxnik.forcechess.bot.ChessBot;
import org.mxnik.forcechess.network.AlphaNet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ChessView {
    public final Stage stage;

    private final String sourcedir = System.getProperty("user.dir") + "/user/src/main/resources/org/mxnik/forcechess/";
    private final String pathToImages = sourcedir + "pieces-basic-png/";
    Group root;
    Constants constants;
    private ChessController controller;
    Group backgroundLayer = new Group();
    private Group pieceLayer = new Group();
    private Group interactionLayer = new Group();
    ImageView winView;

    private Image[] images;

    public ChessView(Stage stage, String fen, int sideLen, String playerStrW, String playerStrB, int playDepth) throws CloneNotSupportedException {
        this.stage = stage;

        setBounds();
        basicInit(sideLen);
        generateImages();

        stage.getScene().widthProperty().addListener((_, number, t1) -> controller.resize());
        stage.getScene().heightProperty().addListener((_, number, t1) -> controller.resize());

        try {
            this.controller = new ChessController(this, stage, fen);
            new Thread(() -> {
                try {
                    initView(playerStrW, playerStrB, fen, playDepth);
                } catch (CloneNotSupportedException e) {
                    throw new IllegalStateException("CloneNotSupportedException was thrown while loading classes. Impossible State, try reinstalling the jar: " + e);
                }
                catch (IllegalStateException e){
                    //TODO: print error message to screen and backoff
                    System.err.println("Model unavailable");
                }
            }).start();
            //this.controller = new ChessController(this, "rnbqkbnrr/ppppppppp/9/9/9/9/9/PPPPPPPPP/RNBQKBNRR w 0 0 0 9");
        }catch (CloneNotSupportedException e){
            throw new CloneNotSupportedException("Error in the chess controller - an invalid clone arose.\nThis is undefined behaviour and should not occur for any reason");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        stage.setOnCloseRequest(e ->
                {
                    cleanUp();
                    Platform.exit();
                    System.exit(0);
                }
        );
    }

    private void initView(String playerStrW, String playerStrB, String fen, int playDepth) throws CloneNotSupportedException {
        Platform.runLater( () -> {
            ProgressIndicator indicator = new ProgressIndicator();
            indicator.setPrefSize(150, 150);
            indicator.setMinSize(150, 150);
            indicator.setLayoutX(constants.MIDDLE_X - indicator.getMinWidth()/2);
            indicator.setLayoutY(constants.MIDDLE_Y - indicator.getMinHeight()/2);
            root.getChildren().add(indicator);
        });
        setPlayers(playerStrW, playerStrB, fen, playDepth);
        Platform.runLater( () -> {
            drawBoard();
            root.getChildren().clear();
            root.getChildren().addAll(backgroundLayer, pieceLayer, interactionLayer);
        });
        this.controller.start();
    }

    private void setPlayers(String playerStrW, String playerStrB, String fen, int playDepth) throws CloneNotSupportedException {
        try {
            if (playerStrW == null && playerStrB == null) {
                this.controller.setPlayers(controller, controller);
            } else if (playerStrW == null) {

                playerStrB = FileLocations.NETWORK_LOCATIONS + playerStrB;
                this.controller.setPlayers(controller, new BatchChessBot(new AlphaNet(ModelSerializer.restoreComputationGraph(playerStrB)), fen, playDepth));
            } else if (playerStrB == null) {
                playerStrW = FileLocations.NETWORK_LOCATIONS + playerStrW;
                this.controller.setPlayers(new BatchChessBot(new AlphaNet(ModelSerializer.restoreComputationGraph(playerStrW)), fen, playDepth), controller);
            } else {
                playerStrW = FileLocations.NETWORK_LOCATIONS + playerStrW;
                playerStrB = FileLocations.NETWORK_LOCATIONS + playerStrB;
                if (playerStrW.equals(playerStrB)) {
                    BatchChessBot bot = new BatchChessBot(new AlphaNet(ModelSerializer.restoreComputationGraph(playerStrW)), fen, playDepth);
                    this.controller.setPlayers(bot, bot);
                } else {
                    this.controller.setPlayers(new BatchChessBot(new AlphaNet(ModelSerializer.restoreComputationGraph(playerStrW)), fen, playDepth), new BatchChessBot(new AlphaNet(ModelSerializer.restoreComputationGraph(playerStrB)), fen, 64));
                }
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    /**
     * sets the x,y and width, height properties
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
        root = new Group();
        Scene scene = new Scene(root, 500, 500, Color.GREY);
        stage.setTitle("Chess");
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
                    square = new ChessBackgroundPane(size, size, true, index);
                } else {
                    square = new ChessBackgroundPane(size, size, false, index);
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

        for (int i = 0; i < b.getBoard().length; i++) {
            int x = i % sideLen;
            int y = i / sideLen;


            ImageView imgView = getImageView(b, i);
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
    private ImageView getImageView(Board b, int i) {
        Piece p = b.getBoard()[i];


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


    /**
     * Shows a Popup-Window that is used to choose the piece to promote to
     * @param white what player is doing this move
     * @param x,y the position of the popup-window
     */
    public void showPromotionStage(boolean white, double x, double y){
        x -= constants.BlockS * 2;
        y -= (double) constants.BlockS / 2;

        int colorOffset = (white?0:1);

        Stage promotionStage = new Stage();

        Group group = new Group();
        HBox box = new HBox();
        ImageView[] promotionOptions = new ImageView[4];
        promotionOptions[0] = new ImageView(images[2+colorOffset]);
        promotionOptions[1] = new ImageView(images[4+colorOffset]);
        promotionOptions[2] = new ImageView(images[6+colorOffset]);
        promotionOptions[3] = new ImageView(images[8+colorOffset]);
        for (int i = 0; i < promotionOptions.length; i++) {
            promotionOptions[i].setFitWidth(constants.BlockS);
            promotionOptions[i].setFitHeight(constants.BlockS);
        }
        box.getChildren().addAll(promotionOptions);

        HBox interaction = new HBox();
        Button[] btns = new Button[4];
        for (int i = 0; i < 4; i++) {
            btns[i] = new Button("");
            int btnID = i;
            btns[i].addEventHandler(ActionEvent.ACTION, event -> controller.handlePromotionPress(btnID, promotionStage));

            btns[i].setPrefSize(constants.BlockS, constants.BlockS);
            btns[i].setMinSize(constants.BlockS, constants.BlockS);
            btns[i].setMaxSize(constants.BlockS, constants.BlockS);

            // IMPORTANT -fx-background-color: transparent;
            btns[i].setStyle("-fx-background-color: transparent");
        }
        interaction.getChildren().addAll(btns);
        group.getChildren().addAll(box, interaction);

        Scene scene = new Scene(group);
        promotionStage.setScene(scene);
        promotionStage.initStyle(StageStyle.UNDECORATED);
        promotionStage.initModality(Modality.APPLICATION_MODAL);
        promotionStage.setResizable(false);
        promotionStage.setX(x);
        promotionStage.setY(y);
        promotionStage.show();
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
