package org.mxnik.forcechess.UI;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;

import javafx.scene.paint.*;

public class Constants {
    final int BoardSize;
    final int WidthStart;
    final int BlockS;
    final Paint DarkColor = Color.DARKBLUE;
    final Paint WhiteColor = Color.WHITE;
    final Rectangle2D bounds;


    public Constants(int sideLen){
        Screen screen = Screen.getPrimary();
        bounds = screen.getVisualBounds();
        BoardSize = (int) bounds.getHeight();
        int middle = (int) bounds.getWidth() / 2;
        WidthStart = middle - BoardSize / 2;
        BlockS = BoardSize / sideLen;
    }

    public Constants(int sideLen, Scene scene){
        Screen screen = Screen.getPrimary();
        bounds = screen.getVisualBounds();
        BoardSize = (int) scene.getHeight();
        int middle = (int) scene.getWidth() / 2;
        WidthStart = middle - BoardSize / 2;
        BlockS = BoardSize / sideLen;
    }
}
