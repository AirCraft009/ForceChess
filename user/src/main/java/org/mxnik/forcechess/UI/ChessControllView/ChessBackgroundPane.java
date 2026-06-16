package org.mxnik.forcechess.UI.ChessControllView;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.mxnik.forcechess.General.SavedSettings;

public class ChessBackgroundPane extends Rectangle {
    private int index;
    private Color primaryColor = Color.DARKBLUE;
    private Color secondaryColor = Color.YELLOW;
    private Color tertiaryColor = Color.RED;
    private boolean active;
    private boolean moved;


    /**
     * A Backgroundpane with a primary and secondary (active, passive)
     * Switching colors based on state;
     *
     * @param v width
     * @param v1 height
     * @param color {@code true == lightSquare}, {@code false == darkSquare}
     * @param index index on the board
     */
    public ChessBackgroundPane(double v, double v1, boolean color, int index) {
        super(v, v1, SavedSettings.savedSettings.lightSquare());
        if(color){
            primaryColor = SavedSettings.savedSettings.lightSquare();
            secondaryColor = SavedSettings.savedSettings.lightHighlight();
            tertiaryColor = SavedSettings.savedSettings.lightMoved();
        }else{
            primaryColor = SavedSettings.savedSettings.darkSquare();
            secondaryColor = SavedSettings.savedSettings.darkHighlight();
            tertiaryColor = SavedSettings.savedSettings.darkMoved();
        }

        this.index = index;
        active = false;
        moved = false;
        updateColors();
    }

    public ChessBackgroundPane(double v, double v1, int index) {
        super(v, v1);
        this.index = index;

    }

    public ChessBackgroundPane(int index) {
        super();
        this.index = index;
    }

    public int getIndex(){
        return index;
    }

    public void toggle(){
        if (!active){
            setActive();
            return;
        }
        deactivate();
    }

    public void setActive(){
        active = true;
        updateColors();
    }

    public void deactivate(){
        active = false;
        updateColors();
    }

    public boolean isActive(){
        return active;
    }

    public void setMoved(){
        moved = true;
        updateColors();
    }

    public void deactivateMoved(){
        moved = false;
        updateColors();
    }

    public boolean isMoved(){
        return moved;
    }

    private void updateColors(){
        if(active){
            this.setFill(secondaryColor);
        }else if(moved){
            this.setFill(tertiaryColor);
        }else {
            this.setFill(primaryColor);
        }
    }
}
