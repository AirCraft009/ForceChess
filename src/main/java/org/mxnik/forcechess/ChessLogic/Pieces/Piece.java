package org.mxnik.forcechess.ChessLogic.Pieces;

public class Piece {
    public static final Piece emptyPiece = new Piece(PieceTypes.EMPTY, (byte) -1, false);

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

    public PieceTypes getType() {
        return type;
    }

    public byte getColor() {
        return color;
    }

    public boolean isHasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public byte[] getMoves(int pos){
        return new byte[0];
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


