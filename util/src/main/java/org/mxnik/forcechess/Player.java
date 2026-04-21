package org.mxnik.forcechess;

public interface Player {
    public MovePacket requestMove(byte[][] possibleMoves);
}
