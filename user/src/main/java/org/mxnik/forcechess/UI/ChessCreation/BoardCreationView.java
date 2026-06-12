package org.mxnik.forcechess.UI.ChessCreation;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.jetbrains.annotations.Nullable;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.UI.ChessControllView.ChessBackgroundPane;
import org.mxnik.forcechess.UI.ChessControllView.ChessButton;
import org.mxnik.forcechess.UI.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mxnik.forcechess.UI.ChessControllView.ChessView.getImageFromRessource;

public class BoardCreationView {
    public final Stage stage;

    private final String sourcedir = "/org/mxnik/forcechess/";
    private final String pathToImages = sourcedir + "pieces-basic-png/";
    BorderPane borderPane;
    Constants constants;
    private BoardCreationController controller;
    Group center = new Group();
    Group backgroundLayer = new Group();
    private Group pieceLayer = new Group();
    private Group interactionLayer = new Group();
    CheckBox white;
    ListView<Group> pieceList;

    TextField nameField;
    Slider sizeSlider;
    Label sizeLabel;
    ListView<HBox> boardsList;
    ArrayList<Button> deleteButtons = new ArrayList<>();
    Button closeButton, saveButton;

    private Image[] images;

    public BoardCreationView(Stage stage) {
        this.stage = stage;

        setBounds();
        basicInit();
        generateImages();

        stage.getScene().widthProperty().addListener((_, number, t1) -> controller.resize());
        stage.getScene().heightProperty().addListener((_, number, t1) -> controller.resize());

        this.controller = new BoardCreationController(this, stage);

        drawListView(true);
        drawControlButtons();
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
     */
    public void basicInit(){
        borderPane = new BorderPane();
        borderPane.setCenter(center);
        Scene scene = new Scene(borderPane, 500, 500, Color.GREY);
        stage.setTitle("Board Creation");
        stage.setScene(scene);
        stage.show();
        constants = new Constants(8, scene);
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

        try {
            for (int i = 0; i < imagePaths.length; i++) {
                images[i] = getImageFromRessource(imagePaths[i]);
            }
        } catch (IOException e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setContentText("Error when reading a piece File. Will close Application");
            error.setOnCloseRequest(
                    event -> {
                        Platform.exit();
                        System.exit(0);
                    }
            );
        }
    }

    /**
     * Responsible for drawing the right hand side of the Scene - The Piece selector and the color selector
     * @param w the currently selected color {@code true == white}, {@code false == black}
     */
    public void drawListView(boolean w){
        if(borderPane.getRight() == null){
            VBox vBox = new VBox();
            white = new CheckBox("White");
            white.setSelected(w);
            white.addEventHandler(Event.ANY, controller);
            pieceList = new ListView<Group>();
            pieceList.getSelectionModel().selectedItemProperty().addListener(controller);
            pieceList.setPrefSize((stage.getScene().getHeight() - white.getHeight()) / 8, (stage.getScene().getHeight() - white.getHeight()));
            vBox.getChildren().addAll(white, pieceList);
            borderPane.setRight(vBox);
        }
        pieceList.getSelectionModel().selectedItemProperty().removeListener(controller);
        int selectedIndex = pieceList.getSelectionModel().getSelectedIndex();
        Group[] pieces = new Group[7];
        String colorPrefix = (w) ? "white-" : "black-";
        String[] arr = {colorPrefix + "pawn.png",
                colorPrefix + "knight.png",
                colorPrefix + "bishop.png",
                colorPrefix + "rook.png",
                colorPrefix + "queen.png",
                colorPrefix + "king.png"};
        int size = (int) ((stage.getScene().getHeight() - white.getHeight()) / 8);
        ChessBackgroundPane bg0 = new ChessBackgroundPane(size, size, false, 0);
        pieces[0] = new Group(bg0);
        for(int i = 0; i < arr.length; i++) {
            ChessBackgroundPane bg = new ChessBackgroundPane(size, size, i%2==0, i+1);

            ImageView piece = new ImageView(images[2*i+(w?0:1)]);
            piece.setFitHeight(size);
            piece.setFitWidth(size);

            pieces[i+1] = new Group(bg, piece);
        }
        ObservableList<Group> observableList = FXCollections.observableList(Arrays.asList(pieces));
        pieceList.setItems(observableList);
        pieceList.getSelectionModel().selectedItemProperty().addListener(controller);
        pieceList.getSelectionModel().select(Math.max(0, selectedIndex));
    }

    /**
     * Responsible for drawing the left hand side of the Scene - The Board selector and the different value changers of the board (Name, Side length)
     */
    private void drawControlButtons(){
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(10);
        Label nameLabel = new Label("Name:");
        grid.add(nameLabel, 0, 0);
        nameField = new TextField();
        nameField.setPromptText("Name");
        grid.add(nameField, 1, 0);

        Label sizeText = new Label("Size:");
        grid.add(sizeText, 0, 1);
        sizeSlider = new Slider();
        sizeSlider.setMin(3);
        sizeSlider.setValue(8);
        sizeSlider.setMax(32);
        sizeSlider.setSnapToTicks(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setMajorTickUnit(5);
        sizeSlider.setMinorTickCount(4);
        sizeSlider.valueProperty().addListener(controller);
        grid.add(sizeSlider, 1, 1);
        sizeLabel = new Label("8");
        grid.add(sizeLabel, 2, 1);

        FenProperties.load();

        boardsList  = new ListView<>();
        boardsList.getSelectionModel().selectedItemProperty().addListener(controller);
        updateBoardList();

        grid.add(boardsList, 0, 2, 2, 1);

        closeButton = new Button("Close");
        closeButton.addEventHandler(Event.ANY, controller);
        grid.add(closeButton, 0, 3);
        saveButton = new Button("Save");
        saveButton.addEventHandler(Event.ANY, controller);
        grid.add(saveButton, 1, 3);

        borderPane.setLeft(grid);
    }

    /**
     * Updates the content of the List of Boards. Must be called if any changes are made to the boards
     */
    void updateBoardList(){
        String[] names;
        if(FenProperties.fenNames.contains("current")){
            Set<String> fenNamesExcl = new HashSet<>(FenProperties.fenNames);
            fenNamesExcl.remove("current");
            System.out.println("Fen names Excl" + fenNamesExcl);
            names = fenNamesExcl.toArray(new String[fenNamesExcl.size()]);
        }else{
            names = FenProperties.fenNames.toArray(new String[FenProperties.fenNames.size()]);
        }
        HBox[] boards = new HBox[names.length];
        deleteButtons.clear();
        for (int i = 0; i < boards.length; i++) {
            boards[i] = new HBox();
            Label name = new Label(names[i]);
            Button button = new Button("Delete");
            button.addEventHandler(Event.ANY, controller);
            deleteButtons.add(button);
            boards[i].getChildren().addAll(name, button);
        }
        ObservableList<HBox> boardsObservable = FXCollections.observableArrayList(boards);
        boardsList.setItems(boardsObservable);
        boardsList.getSelectionModel().selectFirst();
    }


    /**
     * draws the board BackGround
     */
    public void drawBoard() {
        int sideLen = constants.sideLen;
        int size = constants.BlockS;
        int index;
        ChessBackgroundPane[] panes =  new ChessBackgroundPane[sideLen*sideLen];
        ChessButton[] buttons =  new ChessButton[sideLen*sideLen];


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
    public void drawPieces(Board b){ //TODO Extract Method?
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
    private ImageView getImageView(Board b, int i) { //TODO Extract Method??
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
}
