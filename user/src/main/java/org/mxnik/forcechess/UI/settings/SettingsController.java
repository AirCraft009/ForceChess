package org.mxnik.forcechess.UI.settings;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import org.mxnik.forcechess.UI.Constants;
import org.mxnik.forcechess.UI.menu.MenuScene;

import java.util.Optional;

public class SettingsController implements EventHandler<Event>, ChangeListener {
    SettingsView view;
    boolean changed = false;

    SettingsController(SettingsView view) {
        this.view = view;
    }

    /**
     * Handles the button clicks
     * @param event the Event received by the button
     */
    private void handleActionEvent(ActionEvent event) {
        Object source = event.getSource();
        if(source == view.closeButton){
            if(changed){
                SavedSettings.savedSettings = getCurrentSettings();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Save Settings");
                alert.setHeaderText("WARNING: You can't undo this!");
                alert.setContentText("Are you sure you want to change these settings?");
                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == ButtonType.OK){
                    SavedSettings.writeSettings();
                    SavedSettings.loadSavedSettings();
                    new MenuScene(view.stage);
                }
            } else {
                new MenuScene(view.stage);
            }
        }else if(source == view.resetButton){
            new SettingsView(view.stage, true);
        }else if(source == view.defaultButton){
            SavedSettings tempSettings = SavedSettings.savedSettings;
            SavedSettings.savedSettings = SavedSettings.defaultSettings;
            view = new SettingsView(view.stage, false);
            SavedSettings.savedSettings = tempSettings;
            updateButtons();
        }
    }

    @Override
    public void handle(Event event) {
        if(event instanceof ActionEvent){
            handleActionEvent((ActionEvent) event);
        }
    }

    /**
     * Calls {@code updateButtons}
     */
    @Override
    public void changed(ObservableValue observableValue, Object o, Object t1) {
        updateButtons();
    }

    /**
     * If any changes occurred, the view is changed so one can save them
     */
    public void updateButtons(){
        if(!SavedSettings.savedSettings.equals(getCurrentSettings())) {
            changed = true;
            view.closeButton.setTooltip(new Tooltip("Closes the application"));
            view.closeButton.setText("Apply and Close");
            view.resetButton.setVisible(true);
        }else {
            changed = false;
            view.closeButton.setTooltip(null);
            view.closeButton.setText("Close");
            view.resetButton.setVisible(false);
        }
    }

    /**
     * @return The current Settings based on the View
     */
    private SavedSettings getCurrentSettings() {
        String savedPath = view.savePathTF.getText();
        Color lightSquare = view.colorLight.getValue();
        Color darkSquare = view.colorDark.getValue();
        Color lightHighlight = view.highlightLight.getValue();
        Color darkHighlight = view.highlightDark.getValue();
        Color lightMoved = view.movedLight.getValue();
        Color darkMoved = view.movedDark.getValue();
        String defaultBot = view.defaultBot.getValue();
        return new SavedSettings(savedPath, lightSquare, darkSquare, lightHighlight, darkHighlight, lightMoved, darkMoved, defaultBot);
    }

    public void resize(){
        view.constants = new Constants(view.constants.sideLen, view.stage.getScene());
    }
}
