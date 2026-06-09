package org.mxnik.forcechess.UI.menu;

import javafx.event.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import org.mxnik.forcechess.UI.ChessCreation.BoardCreationView;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.settings.SettingsView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static org.mxnik.forcechess.General.FileLocations.NETWORK_LOCATIONS;

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
        }else if(source == menuScene.cBoard){
            new BoardCreationView(menuScene.stage);
        }else if(source == menuScene.settings){
            new SettingsView(menuScene.stage, true);
        }
    }

    private void handleDragEvent(DragEvent event) throws IOException {
        if(!event.getDragboard().hasFiles()){
            return;
        }

        if(event.getEventType() == DragEvent.DRAG_OVER ){
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        } else if (event.getEventType() == DragEvent.DRAG_DROPPED) {

            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                System.out.println(db.getFiles());
                for(File f : db.getFiles()){
                    if(!f.getName().endsWith(".zip"))
                        throw new IllegalArgumentException("Network Files must be Zip files!");


                    Files.copy(
                            f.toPath(),
                            new File(NETWORK_LOCATIONS + "/" + f.getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
                success = true;
            }
            /* let the source know whether the string was successfully
             * transferred and used */
            event.setDropCompleted(success);
        }

        event.consume();
    }

    private void handleDropEvent(DragEvent event){
        if(event.getDragboard().hasFiles()){

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
        if(event.getEventType() == DragEvent.DRAG_OVER
        || event.getEventType() == DragEvent.DRAG_DROPPED) {
            try {
                handleDragEvent((DragEvent) event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
