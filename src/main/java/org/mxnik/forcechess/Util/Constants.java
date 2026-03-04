package org.mxnik.forcechess.Util;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;

import javafx.scene.paint.*;

public class Constants {
    final int BoardSize;
    public final int WidthStart;
    public final int BlockS;
    public final Paint DarkColor = Color.DARKBLUE;
    public final Paint WhiteColor = Color.WHITE;
    private static final Screen screen = Screen.getPrimary();
    public static final Rectangle2D bounds = screen.getVisualBounds();
    public final int sideLen;

    public Constants(int sideLen, Scene scene){
        this.sideLen = sideLen;
        BoardSize = (int) scene.getHeight();
        int middle = (int) scene.getWidth() / 2;
        WidthStart = middle - BoardSize / 2;
        BlockS = BoardSize / sideLen;
    }
}
