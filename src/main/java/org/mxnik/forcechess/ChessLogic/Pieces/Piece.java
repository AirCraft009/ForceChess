package org.mxnik.forcechess.ChessLogic.Pieces;

import org.mxnik.forcechess.ChessLogic.Moves.Helper;

import static org.mxnik.forcechess.ChessLogic.Moves.Helper.*;
import static org.mxnik.forcechess.ChessLogic.Moves.Helper.getCol;

public abstract class Piece {
    //TODO: make EMPTY Piece
    public static final Piece emptyPiece = new Piece(PieceTypes.EMPTY, true, false) {
        @Override
        boolean isValidMove(int from, int to) {
            return false;
        }

        @Override
        byte[] getMoveSet() {
            return new byte[0];
        }
    };

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

    public byte[] getMoves(int pos){
        byte[] mSet = this.getMoveSet();
        byte[] finalMoves = new byte[mSet.length];
        // just sets moves == 0 if they're invalid searching for a better sol.
        for (int i = 0; i < mSet.length; i++) {
            int offset = mSet[i];
            finalMoves[i] = (byte) (mSet[i] * ((isValidMove(pos, pos+offset)? 1 : 0)));
        }
        return finalMoves;
    }


    abstract boolean isValidMove(int from, int to);

    abstract byte[] getMoveSet();

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


