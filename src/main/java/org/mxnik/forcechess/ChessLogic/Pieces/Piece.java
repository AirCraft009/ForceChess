package org.mxnik.forcechess.ChessLogic.Pieces;

import java.util.ArrayList;
import java.util.List;

public abstract class Piece {

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

    public byte[][] getMoves(int pos){
        byte[][] mSets = this.getMoveSet();
        List<byte[]> validDirections = new ArrayList<>();

        for (byte[] mSet : mSets) {
            List<Byte> currentDirection = new ArrayList<>();
            for (byte offset : mSet) {
                int target = pos + offset;
                if (isValidMove(pos, target)) {
                    currentDirection.add((byte) target);
                }
            }
            if (!currentDirection.isEmpty()) {
                byte[] directionMoves = new byte[currentDirection.size()];
                for (int i = 0; i < currentDirection.size(); i++) {
                    directionMoves[i] = currentDirection.get(i);
                }
                validDirections.add(directionMoves);
            }
        }

        return validDirections.toArray(new byte[0][]);
    }


    abstract boolean isValidMove(int from, int to);

    abstract byte[][] getMoveSet();

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
