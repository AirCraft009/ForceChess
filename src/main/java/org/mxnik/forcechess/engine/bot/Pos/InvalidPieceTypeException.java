package org.mxnik.forcechess.engine.bot.Pos;

public class InvalidPieceTypeException extends RuntimeException {
    public InvalidPieceTypeException(String message) {
        super(message);
    }
}
