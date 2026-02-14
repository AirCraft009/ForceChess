package org.mxnik.forcechess.ChessLogic.Pieces;

public final class Piece {
    // Muss eine Klasse sein IF unterstützt nur Konstanten
    private final PieceTypes type;
    // mehr als nur schwarz weiß(wie beim board)
    private final short color;
    private boolean hasMoved;


    public Piece(PieceTypes type, short color, boolean hasMoved){
        this.color = color;
        this.type = type;
        setHasMoved(hasMoved);
    }

    public PieceTypes getType() {
        return type;
    }

    public short getColor() {
        return color;
    }

    public boolean isHasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }
}


