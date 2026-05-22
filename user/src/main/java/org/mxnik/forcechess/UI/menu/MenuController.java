package org.mxnik.forcechess.UI.menu;

import javafx.event.*;
import org.mxnik.forcechess.UI.ChessCreation.BoardCreationScene;
import org.mxnik.forcechess.UI.Constants;

public class MenuController implements EventHandler<Event> {
    MenuScene menuScene;

    public MenuController(MenuScene menuScene) {
        this.menuScene = menuScene;
    }

    /**
     * Handles all button clicks in the menu scene
     * @param event
     */
    private void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();

        if(source == menuScene.pvp){
            new PvPPopup(menuScene.stage);
        }else if(source == menuScene.pvb){
            new PvBPopup(menuScene.stage);
        }else if(source == menuScene.bvb){
            new BvBPopup(menuScene.stage);
        }else if(source == menuScene.train){
            new TrainingPopup(menuScene.stage);
        }else if(source == menuScene.cBoard){
            new BoardCreationScene(menuScene.stage, "rnbqknnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR - - - - 8", 8);
        }
    }

    @Override
    public void handle(Event event) {
        if(event.getEventType() == ActionEvent.ACTION)
            handleActionEvent((ActionEvent) event);
    }

    /**
     * scale the viewed items properly
     */
    public void resize() {
        menuScene.constants = new Constants(menuScene.constants.sideLen, menuScene.getScene());
        menuScene.drawMenu();
    }
}
