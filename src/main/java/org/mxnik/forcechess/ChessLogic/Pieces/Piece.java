package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.MoveList;
import org.mxnik.forcechess.Util.Helper;

import static org.mxnik.forcechess.Util.Helper.*;
import static org.mxnik.forcechess.Util.Helper.getCol;

public abstract class Piece implements Cloneable {
    // Muss eine Klasse sein IF unterstützt nur Konstanten
    protected final PieceTypes type;
    // mehr als nur schwarz weiß(wie beim board)
    protected final boolean color;
    protected boolean hasMoved;


    public Piece(PieceTypes type, boolean color, boolean hasMoved){
        this.color = color;
        this.type = type;
        setHasMoved(hasMoved);
    }

    public Piece(PieceTypes type, boolean color){
        this.color = color;
        this.type = type;
    }
    public boolean getColor() {
        return color;
    }

    public boolean isHasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public void getMoves(int pos, MoveList moveList){
        moveList.startPiece();
        byte[] mSet = this.getMoveSet();

        for (byte moveOffset : mSet) {
            int target = pos + moveOffset * ((color)? 1 : -1);
            if (!isValidMove(pos, target)) {
                continue;
            }

            moveList.startDirection();
            moveList.addMove((byte) target);
        }
    }

    @Override
    public Piece clone() throws CloneNotSupportedException {
        return (Piece) super.clone();
    }


    abstract boolean isValidMove(int from, int to);

    abstract byte[] getMoveSet();

    public abstract int getMaxDir();

    public int getMovesetLen(){
        return getMoveSet().length;
    }

    public boolean isColor() {
        return color;
    }

    public PieceTypes getType() {
        return type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Piece{");
        sb.append("type=").append(type);
        sb.append(", color=").append(color);
        sb.append(", hasMoved=").append(hasMoved);
        sb.append('}');
        return sb.toString();
    }
}

