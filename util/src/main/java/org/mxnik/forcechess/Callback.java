package org.mxnik.forcechess;

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
