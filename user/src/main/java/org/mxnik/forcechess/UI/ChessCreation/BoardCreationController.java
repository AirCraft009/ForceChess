package org.mxnik.forcechess.UI.ChessCreation;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.UI.ChessControllView.ChessBackgroundPane;
import org.mxnik.forcechess.UI.Constants;

public class BoardCreationController implements EventHandler<Event>, ChangeListener<Group> {
    private BoardCreationScene view;
    private Stage stage;
    private Piece[] board;
    private PieceTypes selectedPieceType;

    public BoardCreationController(BoardCreationScene view, Stage stage, String fen) {

    }

    public void resize(){
        view.constants = new Constants(view.constants.sideLen, stage.getScene());
        view.backgroundLayer.getChildren().clear();
        view.clearInteractionLayer();
        view.drawBoard();
        view.drawPieces(board);
    }

    private void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();


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

    @Override
    public void changed(ObservableValue<? extends Group> observableValue, Group group, Group t1) {
        ((ChessBackgroundPane)group.getChildren().getFirst()).deactivate();
        ((ChessBackgroundPane)t1.getChildren().getFirst()).setActive();

        int index = ((ChessBackgroundPane)t1.getChildren().getFirst()).getIndex();
        switch (index){
            case 0 -> selectedPieceType = PieceTypes.PAWN;
            case 1 -> selectedPieceType = PieceTypes.KNIGHT;
            case 2 -> selectedPieceType = PieceTypes.BISHOP;
            case 3 -> selectedPieceType = PieceTypes.ROOK;
            case 4 -> selectedPieceType = PieceTypes.QUEEN;
            case 5 -> selectedPieceType = PieceTypes.KING;
            default -> selectedPieceType = PieceTypes.EMPTY; //TODO error
        }
    }
}
