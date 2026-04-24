package org.mxnik.forcechess.user_bot_interactions;

import org.mxnik.forcechess.global.MovePacket;

public interface Player {
    public MovePacket requestMove();
    public void getMove(MovePacket packet);
}
