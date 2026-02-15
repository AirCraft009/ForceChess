package org.mxnik.forcechess.UI;

import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;

import java.awt.*;

public class Constants {
    final int BoardSize;
    final int WidthStart;
    final int BlockS;
    final Color DarkColor = Color.BLACK;
    final Color WhiteColor = Color.WHITE;
    final Rectangle2D bounds;


    public Constants(int sideLen){
        Screen screen = Screen.getPrimary();
        bounds = screen.getVisualBounds();
        BoardSize = (int) bounds.getHeight();
        int middle = (int) bounds.getWidth() / 2;
        WidthStart = middle - BoardSize / 2;
        BlockS = BoardSize / sideLen;
    }
}
