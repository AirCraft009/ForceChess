package org.mxnik.forcechess.Pos;

public class InvalidPieceTypeException extends RuntimeException {
    public InvalidPieceTypeException(String message) {
        super(message);
    }
}
