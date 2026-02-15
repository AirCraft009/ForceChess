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
    final int heightOffset;


    public Constants(int sideLen, float borderPercent){
        Screen screen = Screen.getPrimary();
        bounds = screen.getVisualBounds();
        BoardSize = (int) (bounds.getHeight() * borderPercent / 100);
        heightOffset = (int) ((bounds.getHeight() - BoardSize) / 2);
        System.out.println(heightOffset);
        int middle = (int) bounds.getWidth() / 2;
        WidthStart = middle - BoardSize / 2;
        BlockS = BoardSize / sideLen;
    }
}
