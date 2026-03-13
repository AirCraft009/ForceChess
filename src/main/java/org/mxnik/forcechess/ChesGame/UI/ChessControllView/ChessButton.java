package org.mxnik.forcechess.ChesGame.UI.ChessControllView;

import javafx.scene.Node;
import javafx.scene.control.Button;


public class ChessButton extends Button {
    private final int field;


    public ChessButton(String s, Node node, int field) {
        super(s, node);
        this.field = field;
    }

    public ChessButton(int field){
        this.field = field;
    }

    /**
     * create a Button with field number
     * This is used to correctly index into a chessboard
     * @param text displayed on the button
     * @param field field number
     */
    public ChessButton(String text, int field){
        super(text);
        this.field = field;
    }

    public int getField(){
        return field;
    }


}
