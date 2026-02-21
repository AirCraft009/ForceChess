package org.mxnik.forcechess.ChessLogic;
import org.mxnik.forcechess.ChessLogic.Moves.MoveChecking;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.Pawn;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

import org.mxnik.forcechess.ChessLogic.Moves.MoveTypes;

public class Board {
    public static int sideLen = 8;
    public static int size = 8;

    Piece[] board;
    byte turn = 0;
    int totalMaterial;
    public int[] teamMaterial;
    public int[] kingIndexes;


    Board(int sideLen, byte playerCount){
        board = new Piece[sideLen*sideLen];
        this.teamMaterial = new int[playerCount];
        this.kingIndexes = new int[playerCount];
        Board.sideLen = sideLen;
        Board.size = sideLen * sideLen;
    }

    Board(String fenString,byte playerCount,  int sideLen){
        BuildFromFen(fenString, sideLen);
        this.teamMaterial = new int[playerCount];
        this.kingIndexes = new int[playerCount];
        Board.sideLen = sideLen;
    }
    /**
     * Board per Fen String aufbauen
     * @param fenStr Der String im Fen format
     * @throws FenException Exception mit position des Errors
     */
    public void BuildFromFen(String fenStr, int sideLen) throws FenException {
        // einfach callen nicht try catch (exception weitergeben)
        board = FenNotation.readFen(fenStr, sideLen);
    }

    /**
     * Gibt das board als Fen String aus
     * @return fenString
     */
    public String WriteAsFen(){
        return FenNotation.writeFen(board);
    }

    public MoveTypes movePiece(int fromindex, int toIndex){
        MoveTypes type = MoveChecking.CheckMove(this, fromindex, toIndex);
        movePieceForced(fromindex, toIndex, type);
        return type;
    }

    private void movePieceForced(int fromindex, int toIndex, MoveTypes type){
        switch (type){
            case Promotion:
                //TODO: make real ToPromote class don't just return a non implemented Piece
                this.board[toIndex] =
                    new Pawn(true, true);
                this.board[fromindex] = Piece.emptyPiece;
                return;
            case GoodMove:
                this.board[toIndex] = this.board[fromindex];
                this.board[fromindex] = Piece.emptyPiece;
                return;
            case KingCastle:
                //TODO: handle castling
            case IllegalMove:
                return;
        }
    }





}
