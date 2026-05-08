package org.mxnik.forcechess.General;

public class FenException extends RuntimeException {
    public FenException(String message, int pos) {
        super("Error when reading Fen String on position:  " + pos + "\nError: " +  message);
    }
}
