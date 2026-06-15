package org.mxnik.forcechess.UI;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;

import javafx.scene.paint.*;

public class Constants {
    final int BoardSize;
    public final int MIDDLE_X, MIDDLE_Y;
    public final int WidthStart;
    public final int HeightStart;
    public final int BlockS;
    public final Paint DarkColor = new Color(0, 0, .2, 1);
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

    public static void defaultStyleButton(Button node, Scene scene, boolean big){
        node.setBackground(new Background(new BackgroundFill(new Color(0,0,0.2, 1), new CornerRadii(scene.getWidth()/190), null)));
        node.setFont(Font.font(null, FontWeight.BOLD, null, scene.getWidth()/((big)?58:120)));
        node.setTextFill(Color.WHITE);
    }
    public static void defaultStyleLabel(Label node, Scene scene){
        node.setFont(Font.font(null, FontWeight.BOLD, null, scene.getWidth()/120));
        node.setTextFill(new Color(0, 0, 0.2, 1));
    }
    public static void defaultStyleChoiceBox(ChoiceBox node, Scene scene){
        node.setBackground(new Background(new BackgroundFill(new Color(0,0,0.2, 1), new CornerRadii(scene.getWidth()/190), null)));
        Platform.runLater(()->{
            Label text = (Label) node.lookup(".label");
            if(text != null){
                text.setFont(Font.font(null, FontWeight.BOLD, null, scene.getWidth()/150));
                text.setTextFill(Color.WHITE);
            }
        });
    }
}
