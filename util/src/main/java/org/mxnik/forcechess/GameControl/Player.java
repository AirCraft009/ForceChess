package org.mxnik.forcechess.GameControl;

import org.mxnik.forcechess.Moves.MovePacket;

import java.io.Closeable;

public interface Player extends Closeable {
    public MovePacket requestMove();
    public void makeMove(MovePacket packet) throws CloneNotSupportedException;
    public void undoMove();
}
