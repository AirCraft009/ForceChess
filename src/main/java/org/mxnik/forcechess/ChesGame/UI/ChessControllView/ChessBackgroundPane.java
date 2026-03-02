package org.mxnik.forcechess.ChesGame.UI.ChessControllView;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class ChessBackgroundPane extends Rectangle {
    private int index;
    private Color primaryColor = Color.DARKBLUE;
    private Color secondaryColor = Color.YELLOW;
    private boolean active;

    public ChessBackgroundPane(double v, double v1, Color primary, Color secondary, int index) {
        super(v, v1, primary);
        primaryColor = primary;
        secondaryColor = secondary;
        this.index = index;
        active = false;
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
            this.setFill(secondaryColor);
            active = true;
            return;
        }
        active = false;
        this.setFill(primaryColor);
    }

    public void setActive(){
        active = true;
        this.setFill(secondaryColor);
    }

    public void deactivate(){
        active = false;
        this.setFill(primaryColor);
    }

    public boolean isActive(){
        return active;
    }
}
