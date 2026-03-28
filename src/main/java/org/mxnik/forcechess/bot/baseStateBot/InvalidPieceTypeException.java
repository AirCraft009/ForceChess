package org.mxnik.forcechess.bot.baseStateBot;

public class InvalidPieceTypeException extends RuntimeException {
    public InvalidPieceTypeException(String message) {
        super(message);
    }
}
