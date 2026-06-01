package org.mxnik.forcechess.UI;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;

import javafx.scene.paint.*;

public class Constants {
    final int BoardSize;
    public final int MIDDLE_X, MIDDLE_Y;
    public final int WidthStart;
    public final int HeightStart;
    public final int BlockS;
    public final Paint DarkColor = Color.DARKBLUE;
    public final Paint WhiteColor = Color.WHITE;
    private static final Screen screen = Screen.getPrimary();
    public static final Rectangle2D bounds = screen.getVisualBounds();
    public final int sideLen;

    public Constants(int sideLen, Scene scene){
        this.sideLen = sideLen;
        BoardSize = (int) (Math.min(scene.getHeight(), scene.getWidth()) * 0.95);
        MIDDLE_X = (int) scene.getWidth() / 2;
        MIDDLE_Y = (int) scene.getHeight() / 2;
        WidthStart = MIDDLE_X - BoardSize / 2;
        HeightStart = MIDDLE_Y - BoardSize / 2;
        BlockS = BoardSize / sideLen;
    }
}
