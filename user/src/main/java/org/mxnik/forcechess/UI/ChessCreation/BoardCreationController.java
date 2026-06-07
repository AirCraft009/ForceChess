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
import org.mxnik.forcechess.ChessLogic.Notation.FenReader;
import org.mxnik.forcechess.ChessLogic.Notation.FenWriter;
import org.mxnik.forcechess.ChessLogic.Pieces.*;
import org.mxnik.forcechess.FileHandling.FenProperties;
import org.mxnik.forcechess.UI.ChessControllView.ChessBackgroundPane;
import org.mxnik.forcechess.UI.ChessControllView.ChessButton;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.menu.MenuScene;

import java.util.Arrays;

public class BoardCreationController implements EventHandler<Event>, ChangeListener {
    private BoardCreationView view;
    private Stage stage;
    private Piece[] board;
    private int blackKPos = -1, whiteKPos = -1;
    private PieceTypes selectedPieceType;

    public BoardCreationController(BoardCreationView view, Stage stage) {
        this.view = view;
        this.stage = stage;
        board = new Piece[view.constants.sideLen * view.constants.sideLen];
        Arrays.fill(board, EmptyPiece.EMPTY_PIECE);
    }

    /**
     * Handle Clicks on the Board, and places pieces, if possible
     * @param source the ChessButton that was clicked
     */
    private void handleCButtonClick(ChessButton source) {
        int square = source.getField();
        if(square == whiteKPos){
            whiteKPos = -1;
        }else if(square == blackKPos){
            blackKPos = -1;
        }
        board[square] = switch (selectedPieceType){
            case EMPTY -> EmptyPiece.EMPTY_PIECE;
            case PAWN -> new Pawn(view.white.isSelected(), false);
            case KNIGHT -> new Knight(view.white.isSelected(), false);
            case BISHOP -> new Bishop(view.white.isSelected(), false);
            case ROOK -> new Rook(view.white.isSelected(), false);
            case QUEEN -> new Queen(view.white.isSelected(), false);
            case KING -> {
                boolean color = view.white.isSelected();
                int sideLen = view.constants.sideLen;
                int[] illegalPos = new int[]{square+1, square-1, square+sideLen, square+sideLen+1, square+sideLen-1, square-sideLen, square-sideLen+1, square-sideLen-1};
                boolean legal = true;
                for(int i : illegalPos){
                    if (i / sideLen == square / sideLen && i % sideLen == square % sideLen) {
                        legal = false;
                        break;
                    }
                }

                if(((color)?whiteKPos:blackKPos)==-1 && legal){
                    if(color){
                        whiteKPos = square;
                    }else{
                        blackKPos = square;
                    }
                    yield new King(view.white.isSelected(), false);
                }else {
                    yield EmptyPiece.EMPTY_PIECE;
                }
            }
            default -> board[square];
        };

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
            if(text != null && !text.isBlank() && !text.equals("default")){
                FenProperties.addFenStr(text, FenWriter.WriteFen(board, true, (int) Math.round(view.sizeSlider.getValue()), -1));
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
        int size = (int) Math.round(view.sizeSlider.getValue());
        if(size*size != board.length)
            board = new Piece[size*size];
        Arrays.fill(board, EmptyPiece.EMPTY_PIECE);
        view.sizeLabel.setText(String.valueOf(size));
        view.constants = new Constants(size, stage.getScene());
        whiteKPos = -1;
        blackKPos = -1;
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
        board = reader.readFenBoard();
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
