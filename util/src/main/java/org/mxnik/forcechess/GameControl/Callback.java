package org.mxnik.forcechess.GameControl;

import org.mxnik.forcechess.Moves.GameState;

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
