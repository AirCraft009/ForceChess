package org.mxnik.forcechess.ChessLogic;
import org.mxnik.forcechess.ChessLogic.Moves.MoveChecking;
import org.mxnik.forcechess.ChessLogic.Moves.MoveList;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.ChessLogic.Pieces.Pawn;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;

import org.mxnik.forcechess.ChessLogic.Moves.MoveTypes;

public class Board {
    public static int sideLen = 8;
    public static int size = 8;

    Piece[] board;
    boolean turn = true;
    int totalMaterial = 0;
    int maxDirs = 0;
    public int amountPieces = 0;
    MoveList moveList;
    public int[] teamMaterial;
    public int[] kingIndexes;
    int maxMoves = 0;

    
    public Board(int sideLen, byte playerCount){
        board = new Piece[sideLen*sideLen];
        this.teamMaterial = new int[playerCount];
        this.kingIndexes = new int[playerCount];
        Board.sideLen = sideLen;
        Board.size = sideLen * sideLen;
    }

    public Board(String fenString,byte playerCount,  int sideLen){
        BuildFromFen(fenString, sideLen);
        this.teamMaterial = new int[playerCount];
        this.kingIndexes = new int[playerCount];
        Board.sideLen = sideLen;
        amountPieces = 0;
    }
    /**
     * Board per Fen String aufbauen
     * @param fenStr Der String im Fen format
     * @throws FenException Exception mit position des Errors
     */
    public void BuildFromFen(String fenStr, int sideLen) throws FenException {
        // einfach callen nicht try catch (exception weitergeben)
        FenNotation notation = new FenNotation(fenStr);
        board = notation.readFenBoard();
        turn = notation.readFenTurn();

        for (int i = 0; i < board.length; i++) {
            Piece p = board[i];
            if (p == EmptyPiece.EMPTY_PIECE)
                continue;

            totalMaterial += p.getType().value;
            amountPieces ++;
            maxDirs += p.getMaxDir();
            maxMoves += p.getMovesetLen();
        }

        moveList = new MoveList(amountPieces, maxDirs, maxMoves);
    }

    public void loadMoveFromPosition(){
        moveList.clear();
        for (int i = 0; i < board.length; i++) {
            if (board[i] == EmptyPiece.EMPTY_PIECE){
                continue;
            }

            board[i].getMoves(i, moveList);

        }
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
                this.board[fromindex] = EmptyPiece.EMPTY_PIECE;
                return;
            case GoodMove:
                this.board[toIndex] = this.board[fromindex];
                this.board[fromindex] = EmptyPiece.EMPTY_PIECE;
                return;
            case KingCastle:
                //TODO: handle castling
            case IllegalMove:
                return;
        }
    }


    public Piece[] getBoard() {
        return board;
    }

    public void setBoard(Piece[] board) {
        this.board = board;
    }

    public static void main(String[] args) {
        Board board1 = new Board("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8", (byte) 2, 8);
        MoveChecking.CheckMove(board1, 0, 3);
    }
}
