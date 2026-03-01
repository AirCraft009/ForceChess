package org.mxnik.forcechess.UI;

import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class ChessBackgroundPane extends Rectangle {
    private int index;

    public ChessBackgroundPane(double v, double v1, Paint paint, int index) {
        super(v, v1, paint);
        this.index = index;
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
}
