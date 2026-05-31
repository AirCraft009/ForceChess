package org.mxnik.forcechess.UI.menu;

import javafx.event.*;
import javafx.scene.input.KeyEvent;
import org.mxnik.forcechess.UI.ChessCreation.BoardCreationView;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.settings.SettingsView;

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
            new BoardCreationView(menuScene.stage);
        }else if(source == menuScene.settings){
            new SettingsView(menuScene.stage);
        }
    }

    private void handleKeyEvent(KeyEvent event) {
        switch (event.getCode()){
            case F11 -> menuScene.stage.setFullScreen(!menuScene.stage.isFullScreen());
        }
    }

    @Override
    public void handle(Event event) {
        if(event.getEventType() == ActionEvent.ACTION)
            handleActionEvent((ActionEvent) event);
        if(event.getEventType() == KeyEvent.KEY_PRESSED){
            handleKeyEvent((KeyEvent)event);
        }
    }

    /**
     * scale the viewed items properly
     */
    public void resize() {
        menuScene.constants = new Constants(menuScene.constants.sideLen, menuScene.getScene());
        menuScene.root.getChildren().clear();
        menuScene.drawMenu();
    }
}
