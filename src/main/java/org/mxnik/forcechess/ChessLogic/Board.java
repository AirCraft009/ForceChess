package org.mxnik.forcechess.ChessLogic;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;

public class Board {
    Piece[] board;
    short turn = 0;


    Board(int sideLen){
        board = new Piece[sideLen*sideLen];
    }

    Board(String fenString){
        BuildFromFen(fenString);
    }

    /**
     * Board per Fen String aufbauen
     * @param fenStr Der String im Fen format
     * @throws FenException Exception mit position des Errors
     */
    public void BuildFromFen(String fenStr) throws FenException {
        // einfach callen nicht try catch (exception weitergeben)
        board = FenNotation.readFen(fenStr);
    }

    /**
     * Gibt das board als Fen String aus
     * @return fenString
     */
    public String WriteAsFen(){
        return FenNotation.writeFen(board);
    }
}
