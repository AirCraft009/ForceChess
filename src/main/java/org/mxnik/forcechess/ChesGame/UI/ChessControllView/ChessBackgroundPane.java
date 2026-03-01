package org.mxnik.forcechess.ChesGame.UI.ChessControllView;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class ChessBackgroundPane extends Rectangle {
    private int index;
    private Color primaryColor;
    private Color secondaryColor;
    private boolean active;

    public ChessBackgroundPane(double v, double v1, Paint paint, int index) {
        super(v, v1, paint);
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
}
