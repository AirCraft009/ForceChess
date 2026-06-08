package org.mxnik.forcechess.UI.ChessCreation;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Notation.FenReader;
import org.mxnik.forcechess.ChessLogic.Notation.FenWriter;
import org.mxnik.forcechess.ChessLogic.Pieces.*;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.UI.ChessControllView.ChessBackgroundPane;
import org.mxnik.forcechess.UI.ChessControllView.ChessButton;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.menu.MenuScene;

import java.util.Arrays;

public class BoardCreationController implements EventHandler<Event>, ChangeListener {
    private BoardCreationView view;
    private Stage stage;
    private Board board;
    private PieceTypes selectedPieceType;
    private final String BASE_FEN = "8/8/8/8/8/8/8/8 w - - 0 8";

    public BoardCreationController(BoardCreationView view, Stage stage) {
        this.view = view;
        this.stage = stage;
        board = new Board(BASE_FEN);
    }

    /**
     * Handle Clicks on the Board, and places pieces, if possible
     * @param source the ChessButton that was clicked
     */
    private void handleCButtonClick(ChessButton source) {
        Piece[] boardPieces = board.getBoard();
        boolean color = view.white.isSelected();
        int square = source.getField();
        if(square == board.getKingWPos()){
            board.setKingWPos(-1);
        }else if(square == board.getKingBPos()){
            board.setKingBPos(-1);
        }
        boardPieces[square] = switch (selectedPieceType){
            case EMPTY -> EmptyPiece.EMPTY_PIECE;
            case PAWN -> new Pawn(color, false);
            case KNIGHT -> new Knight(color, false);
            case BISHOP -> new Bishop(color, false);
            case ROOK -> new Rook(color, false);
            case QUEEN -> new Queen(color, false);
            case KING -> {
                if(((color)?board.getKingWPos()==-1:board.getKingBPos()==-1) && !board.isChecked(square, color)){
                    if(color){
                        board.setKingWPos(square);
                    }else{
                        board.setKingBPos(square);
                    }
                    yield new King(color, false);
                }else {
                    yield boardPieces[square];
                }
            }
            default -> boardPieces[square];
        };

        board.setBoard(boardPieces);

        if(board.isChecked((color)?board.getKingBPos(): board.getKingWPos(), !color)){
            boardPieces[square] = EmptyPiece.EMPTY_PIECE;
            board.setBoard(boardPieces);
        }

        view.drawPieces(board);
    }

    /**
     * responsible for deleting boards
     * @param source source button
     */
    private void handleDelButtonClick(Button source) {
        int index = view.deleteButtons.indexOf(source);
        String name = (String) FenProperties.fenNames.toArray()[index];
        FenProperties.removeFenStr(name);
        view.updateBoardList();
    }

    /**
     * Handles Button clicks of any kind
     * @param event the event recieved by the button
     */
    private void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();

        if(source instanceof ChessButton chessButton) {
            handleCButtonClick(chessButton);
        }else if(source == view.white){
            view.drawListView(view.white.isSelected());
        }else if(source == view.closeButton){
            new MenuScene(stage);
        }else if(source == view.saveButton){
            String text = view.nameField.getText();
            if(text != null && !text.isBlank() && !text.equals("default") && board.getKingWPos() >= 0 && board.getKingBPos() >= 0){
                FenProperties.addFenStr(text, FenWriter.WriteFen(board));
                view.updateBoardList();
            }
        }else if(source instanceof Button deleteButton){
            if(!deleteButton.getText().equals("Delete"))
                return;
            handleDelButtonClick(deleteButton);
        }
    }

    private void handleMouseEvent(MouseEvent event) {

    }

    private void handleKeyEvent(KeyEvent event) {
        Object source = event.getSource();
        if(event.getEventType() != KeyEvent.KEY_PRESSED){
            return;
        }

        switch (event.getCode()){
            case F11 -> stage.setFullScreen(!stage.isFullScreen());
        }
    }

    @Override
    public void handle(Event event) {
        if(event instanceof ActionEvent)
            handleActionEvent((ActionEvent)event);
        else if(event instanceof MouseEvent)
            handleMouseEvent((MouseEvent)event);
        else if(event instanceof KeyEvent)
            handleKeyEvent((KeyEvent)event);
    }

    /**
     * Handles Clicks on the Piece selector
     * @param observableValue
     * @param oldValue
     * @param newValue
     */
    private void changedPieceList(ObservableValue<? extends Group> observableValue, Group oldValue, Group newValue) {
        if(oldValue != null)
            ((ChessBackgroundPane)oldValue.getChildren().getFirst()).deactivate();
        ((ChessBackgroundPane)newValue.getChildren().getFirst()).setActive();

        int index = ((ChessBackgroundPane)newValue.getChildren().getFirst()).getIndex();
        switch (index){
            case 0 -> selectedPieceType = PieceTypes.EMPTY;
            case 1 -> selectedPieceType = PieceTypes.PAWN;
            case 2 -> selectedPieceType = PieceTypes.KNIGHT;
            case 3 -> selectedPieceType = PieceTypes.BISHOP;
            case 4 -> selectedPieceType = PieceTypes.ROOK;
            case 5 -> selectedPieceType = PieceTypes.QUEEN;
            case 6 -> selectedPieceType = PieceTypes.KING;
            default -> selectedPieceType = PieceTypes.ILLEGAL; //TODO error
        }
    }

    /**
     * Handles changes to the Board size slider
     * @param observableValue
     * @param oldValue
     * @param newValue
     */
    private void changedSizeSlider(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
        Piece[] boardPieces = board.getBoard();
        int size = (int) Math.round(view.sizeSlider.getValue());
        if(size*size == boardPieces.length) {
            return;
        }
        Board.setSideLen(size);
        boardPieces = new Piece[size * size];
        Arrays.fill(boardPieces, EmptyPiece.EMPTY_PIECE);
        board.setBoard(boardPieces);
        view.sizeLabel.setText(String.valueOf(size));
        view.constants = new Constants(size, stage.getScene());
        board.setKingWPos(-1);
        board.setKingBPos(-1);
        resize();
    }

    /**
     * Handles the selection of different Boards
     * @param observableValue
     * @param oldValue
     * @param newValue
     */
    private void changedBoard(ObservableValue<? extends HBox> observableValue, HBox oldValue, HBox newValue) {
        String fenName = ((Label)newValue.getChildren().getFirst()).getText();
        FenReader reader = new FenReader(FenProperties.getFenStr(fenName));
        view.sizeSlider.setValue(reader.readSideLen());
        view.nameField.setText(fenName);
        Board.setSideLen(reader.getBoardLenght());
        board.setBoard(reader.readFenBoard());
        DiversePair<Integer, Integer> kPos = reader.readKingPos();
        board.setKingWPos(kPos.first());
        board.setKingBPos(kPos.second());
        resize();
    }

    @Override
    public void changed(ObservableValue observableValue, Object oldValue, Object newValue) {
        if(newValue instanceof Group){
            changedPieceList(observableValue, (Group)oldValue, (Group)newValue);
        }else if(newValue instanceof Number){
            changedSizeSlider(observableValue, (Number)oldValue, (Number)newValue);
        }else if(newValue instanceof HBox){
            changedBoard(observableValue, (HBox)oldValue, (HBox)newValue);
        }
    }

    public void resize(){
        view.constants = new Constants(view.constants.sideLen, stage.getScene());
        view.backgroundLayer.getChildren().clear();
        view.clearInteractionLayer();
        view.drawBoard();
        view.drawPieces(board);
    }
}
