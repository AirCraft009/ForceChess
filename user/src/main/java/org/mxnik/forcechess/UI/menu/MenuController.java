package org.mxnik.forcechess.UI.menu;

import javafx.event.*;
import org.mxnik.forcechess.UI.Constants;

public class MenuController implements EventHandler<Event> {
    MenuScene menuScene;

    public MenuController(MenuScene menuScene) {
        this.menuScene = menuScene;
    }

    private void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();

        if(source == menuScene.pvp){
            new PvPPopup();
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
