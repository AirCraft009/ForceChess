package org.mxnik.forcechess.ChessLogic.Pieces;

public final class Piece {
    public static final Piece emptyPiece = new Piece(PieceTypes.EMPTY, (short) -1, false);

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

    public Piece(PieceTypes type, short color){
        this.color = color;
        this.type = type;
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


