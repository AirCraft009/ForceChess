package org.mxnik.forcechess.UI.settings;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.menu.MenuScene;

public class SettingsController implements EventHandler<Event>, ChangeListener {
    SettingsView view;
    boolean changed = false;

    SettingsController(SettingsView view) {
        this.view = view;
    }

    private void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();
        if(source == view.closeButton){
            if(changed){
                SavedSettings.savedSettings = getCurrentSettings();
                SavedSettings.writeSettings();
            } else {
                new MenuScene(view.stage);
            }
        }else if(source == view.resetButton){
            new SettingsView(view.stage);
        }else if(source == view.defaultButton){
            SavedSettings.savedSettings = SavedSettings.defaultSettings;
            new SettingsView(view.stage);
        }
    }

    @Override
    public void handle(Event event) {
        if(event instanceof ActionEvent){
            handleActionEvent((ActionEvent) event);
        }
    }

    @Override
    public void changed(ObservableValue observableValue, Object o, Object t1) {
        if(t1 instanceof Boolean) {
            view.useFP16.setDisable(!(boolean)t1);
        }
        if(!SavedSettings.savedSettings.equals(getCurrentSettings())) {
            changed = true;
            view.closeButton.setText("Apply and Close");
            view.resetButton.setVisible(true);
        }else {
            changed = false;
            view.closeButton.setText("Close");
            view.resetButton.setVisible(false);
        }
    }
    private SavedSettings getCurrentSettings() {
        String savedPath = view.savePathTF.getText();
        Color lightSquare = view.colorLight.getValue();
        Color darkSquare = view.colorDark.getValue();
        Color lightHighlight = view.highlightLight.getValue();
        Color darkHighlight = view.highlightDark.getValue();
        Color lightMoved = view.movedLight.getValue();
        Color darkMoved = view.movedDark.getValue();
        boolean gpu = view.useGPU.isSelected();
        boolean fp16 = view.useFP16.isSelected();
        String defaultBot = view.defaultBot.getValue();
        return new SavedSettings(savedPath, lightSquare, darkSquare, lightHighlight, darkHighlight, lightMoved, darkMoved, gpu, fp16, defaultBot);
    }

    public void resize(){
        view.constants = new Constants(view.constants.sideLen, view.stage.getScene());
    }
}
