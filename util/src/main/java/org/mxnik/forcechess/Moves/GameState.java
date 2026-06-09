package org.mxnik.forcechess.Moves;

public enum GameState {
    Continue,
    StaleMate,
    CheckMate,
    FiftyMove,
    Material,
    Resignation;

    public final String reason;

    GameState(){
        reason = "Reason for Game End: " + this;
    }

}
