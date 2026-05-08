package org.mxnik.forcechess.GameControl;

import org.mxnik.forcechess.Moves.MovePacket;

public interface Player {
    public MovePacket requestMove();
    public void getMove(MovePacket packet);
}
