package org.mxnik.forcechess;

public interface Player {
    public MovePacket requestMove();
    public void getMove(MovePacket packet);
}
