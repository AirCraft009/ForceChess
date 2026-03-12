package org.mxnik.forcechess.ChessLogic;
import org.mxnik.forcechess.ChessLogic.Moves.MoveList;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.*;

import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.Util.FastBitmap;
import org.mxnik.forcechess.Util.Helper;

import java.util.Arrays;

public class Board {
    public static int sideLen = 8;
    public static int size = 64;

    Piece[] board;
    boolean turn = true;
    int totalMaterial = 0;
    int maxDirs = 0;
    public int amountPieces;
    MoveList moveList;
    public int teamWMaterial;
    public int teamBMaterial;
    private DiversePair<King, Integer> kingWPos;
    private DiversePair<King, Integer> kingBPos;
    int maxMoves = 0;

    
    public Board(int sideLen){
        board = new Piece[sideLen*sideLen];
        Board.sideLen = sideLen;
        Board.size = sideLen * sideLen;
    }

    public Board(String fenString){
        BuildFromFen(fenString);
    }
    /**
     * Board per Fen String aufbauen
     * @param fenStr Der String im Fen format
     * @throws FenException Exception mit position des Errors
     */
    public void BuildFromFen(String fenStr) throws FenException {
        // einfach callen nicht try catch (exception weitergeben)
        FenNotation notation = new FenNotation(fenStr);
        board = notation.readFenBoard();
        DiversePair<DiversePair<King, Integer>, DiversePair<King, Integer>> KingPositions = notation.readKingPos();
        kingWPos = KingPositions.first();
        kingBPos = KingPositions.second();
        turn = notation.readFenTurn();
        sideLen = notation.readSideLen();

        for (int i = 0; i < board.length; i++) {
            Piece p = board[i];
            if (p == EmptyPiece.EMPTY_PIECE)
                continue;

            totalMaterial += p.getType().value;
            amountPieces ++;
            maxDirs += p.getMaxDir();
            maxMoves += p.getMovesetLen();
        }
        Board.size = board.length;
        moveList = new MoveList(amountPieces, maxDirs, maxMoves);
    }

    /**
     * returned alle Moves in einem diversePair
     * element first ist sind die Moves element second der offset der figur am Schachbrett
     *
     * Unoptimiert aber funktionierend
     * @return
     */
    public byte[][] getMovesFromPosition(){
        moveList.clear();
        byte[] moves = moveList.getMovesArray();

        int whiteKingPieceCount = 0;
        int blackKingPieceCount = 0;


        // warning weil der Compiler sich nicht sicher sein kann
        // ist aber type safe während Runtime
        byte[][] legalMoves = new byte[size][];


        int pieceCount = 0;
        int prevMovecount = moveList.getMoveCount();

        for (int i = 0; i < board.length; i++) {
            // leere Felder skippen
            // könige Skippen ->
            // dannach berechnen um Schach moves zu verhindern
            if (board[i].getType() == PieceTypes.EMPTY) {
                legalMoves[i] = new byte[0];
                continue;
            }

            board[i].getMoves(i, moveList);
            if (board[i].getColor() && board[i].getType() == PieceTypes.KING){
                whiteKingPieceCount = pieceCount;
                legalMoves[i] = new byte[0];
                pieceCount ++;
                continue;
            } else if (board[i].getType() == PieceTypes.KING) {
                blackKingPieceCount = pieceCount;
                legalMoves[i] = new byte[0];
                pieceCount ++;
                continue;
            }

            //System.out.println("i: " + pieceCount +" piece: " +board[i]);
            int newMoveCount = moveList.getMoveCount();

            int dirStart = moveList.getDirectionOffset(pieceCount);
            int dirCount = moveList.getDirectionCount(pieceCount);

            // keep track of the current position in the final moves arr
            int ptr = 0;
            byte[] legalMoveSection = new byte[newMoveCount - prevMovecount];
            prevMovecount = newMoveCount;


            directionLoop:
            for (int d = 0; d < dirCount; d++) {

                int dirIndex = dirStart + d;

                int moveOffset = moveList.getDirectionMovesOffset(dirIndex);
                int moveLength = moveList.getDirectionMovesLength(dirIndex);

                int j;

                for (j = 0; j < moveLength; j++) {

                    byte square = moves[moveOffset + j];

                    // hideous
                    if (board[i].getType() == PieceTypes.PAWN){
                        // hideous
                        if(Helper.isDiagonalMove(i, square)) {
                            if (board[square].getColor() != board[i].getColor() && board[square] != EmptyPiece.EMPTY_PIECE) {
                                legalMoveSection[ptr + j] = square;
                                ptr++;
                            }
                            break;
                        } else{
                            if (board[square] != EmptyPiece.EMPTY_PIECE) {
                                break;
                            }
                        }
                    }else if(board[i].getType() == PieceTypes.KING){
                        System.out.println("King move recognized");
                        int dir = Integer.compare(square, i);
                        Piece corner = (dir < 0)?board[i - Helper.distanceLeftB(i)]:board[i + Helper.distanceRightB(i)];
                        // hideous
                        if(Helper.colDiff(i, square) > 1) {
                            System.out.println("Test casteling");
                            if (board[i].isHasMoved() || corner.isHasMoved() || corner.getType()!=PieceTypes.ROOK) {
                                System.out.println("Not casteling");
                                break;
                            }

                            // can't be out of bounds because the move won't be registered
                            for(int k = i+dir; k != square; k+=dir){
                                if(board[k] != EmptyPiece.EMPTY_PIECE){
                                    System.out.println("Empty Pieces");
                                    break directionLoop;
                                }
                            }
                            legalMoveSection[ptr + j] = square;
                            ptr++;
                            break;
                        }
                    }

                    if (board[square] != EmptyPiece.EMPTY_PIECE) {
                        if(board[square].getColor() != board[i].getColor()) {
                            // blocked → stop this direction
                            legalMoveSection[ptr + j] = square;
                            ptr++;
                        }
                        break;
                    }

                    legalMoveSection[ptr + j] = square;
                    //System.out.printf("legalMove: %d -> %d\n", i, square);
                    //otherwise square is free → legal move
                }
                ptr += j;
            }
            legalMoves[i] = Arrays.copyOf(legalMoveSection, ptr);
            pieceCount ++;
        }

        // handle whiteKing
        addKingMove(legalMoves, moves, true, whiteKingPieceCount, kingWPos.second());
        //addKingMove(legalMoves, moves,  false, blackKingPieceCount, kingBPos.second());
        return legalMoves;
    }

    public void addKingMove(byte[][] legalMoves, byte[] moves, boolean color, int kingPieceCount, int kingPos){
        // keine moves in ein Schachfeld

        int dirStart = moveList.getDirectionOffset(kingPieceCount);
        int dirCount = moveList.getDirectionCount(kingPieceCount);
        byte[] legalMoveSection = new byte[King.dirCount];
        int movePtr = 0;
        int dirIndex;

        for (int i = 0; i < dirCount; i++) {
            dirIndex = dirStart+i;

            int moveOffset = moveList.getDirectionMovesOffset(dirIndex);
            int moveLength = moveList.getDirectionMovesLength(dirIndex);

            int j;
            //alle moves durchgehen
            moveLoop:
            for (j = 0; j < moveLength; j++) {
                byte square = moves[moveOffset + j];
                System.out.println(square);

                // alle anderen moves um für mögliches Schach zu checken

                int dir = Integer.compare(square, kingPos);
                Piece corner = (dir < 0)?board[kingPos - Helper.distanceLeftB(kingPos)]:board[kingPos + Helper.distanceRightB(kingPos)];

                // hideous
                if(Helper.colDiff(kingPos, square) > 1) {

                    if (board[kingPos].isHasMoved() || corner.isHasMoved() || corner.getType() != PieceTypes.ROOK) {
                        System.out.println("Not casteling");
                        break;
                    }

                    // can't be out of bounds because the move won't be registered
                    for (int k = kingPos + dir; k != square; k += dir) {
                        if (board[k] != EmptyPiece.EMPTY_PIECE) {
                            System.out.println("Empty Pieces");
                            break moveLoop;
                        }
                    }
                }else if(board[square].getColor() == color && board[square] != EmptyPiece.EMPTY_PIECE) {
                    break;
                }

                for (int k = 0; k <legalMoves.length; k++) {
                    if(board[k].getColor() == color) {
                        continue;
                    }
                    byte[] posMoves = legalMoves[k];
                    for (byte endSquare : posMoves){
                        if (endSquare == square){
                            continue moveLoop;
                        }
                    }
                }
                legalMoveSection[movePtr] = square;
                movePtr++;
            }
        }
        legalMoves[kingPos] = Arrays.copyOf(legalMoveSection, movePtr);
    }

    /**
    public kingM(){
        System.out.println("King move recognized");
        int dir = Integer.compare(square, i);
        Piece corner = (dir < 0)?board[i - Helper.distanceLeftB(i)]:board[i + Helper.distanceRightB(i)];
        // hideous
        if(Helper.colDiff(i, square) > 1) {
            System.out.println("Test casteling");
            if (board[i].isHasMoved() || corner.isHasMoved() || corner.getType()!=PieceTypes.ROOK) {
                System.out.println("Not casteling");
                break;
            }

            // can't be out of bounds because the move won't be registered
            for(int k = i+dir; k != square; k+=dir){
                if(board[k] != EmptyPiece.EMPTY_PIECE){
                    System.out.println("Empty Pieces");
                    break directionLoop;
                }
            }
            legalMoveSection[ptr + j] = square;
            ptr++;
            break;
        }
    }
     */

    public void move(int from , int to) throws CloneNotSupportedException {
        if(board[from] == EmptyPiece.EMPTY_PIECE){
            return;
        }
        if(board[to] != EmptyPiece.EMPTY_PIECE){
            amountPieces -= 1;
        }
        Piece p = board[from].clone();
        p.setHasMoved(true);
        board[from] = EmptyPiece.EMPTY_PIECE;
        board[to] = p;

        if (p.getType() != PieceTypes.KING){
            return;
        }

        if(Helper.colDiff(from, to) > 1){
            int dir = Integer.compare(to, from);
            int rookPos = (dir < 0)?from - Helper.distanceLeftB(from):from + Helper.distanceRightB(from);
            move(rookPos, to - dir);
        }
    }


    /**
     * Gibt das board als Fen String aus
     * @return fenString
     */
    public String WriteAsFen(){
        return FenNotation.writeFen(board);
    }


    public Piece[] getBoard() {
        return board;
    }

    public void setBoard(Piece[] board) {
        this.board = board;
    }

    public static void main(String[] args) {
        Board board1 = new Board("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        long starT = System.nanoTime();
        byte[][] allMoves = null;
        for (int i = 0; i < 1000000; i++) {
            allMoves = board1.getMovesFromPosition();
        }
        long endT = System.nanoTime();
        long timeT = endT - starT;

        System.out.println(board1.kingWPos);
        System.out.println(board1.kingBPos);
        System.out.println("-----------------------");

        System.out.printf("took time for full 1000000: %dns\n" +
                "avg time per board: %dns\n" +
                "so on avg %d per sec\n",
                timeT,
                timeT / 1000000,
                100000000 / (timeT / 1000000));

        System.out.println("-----------------------");
//        for (int i = 0; i < allMoves.length; i++) {
//            System.out.printf("%d can move to %s\n", i, Arrays.toString(allMoves[i]));
//        }
    }
}
