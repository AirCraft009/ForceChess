package org.mxnik.forcechess.user_bot_interactions;

import org.mxnik.forcechess.global.GameState;

public interface Callback {

    /**
     * called after every move played to the authoritative visual
     */
    public void update();

    /**
     * called after a game is terminated (win/loss/stalemate);
     */
    public void finish(GameState gameState);
}
