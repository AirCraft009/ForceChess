package org.mxnik.forcechess.engine.bot.baseStateBot;

public class InvalidPieceTypeException extends RuntimeException {
    public InvalidPieceTypeException(String message) {
        super(message);
    }
}
