package org.mxnik.forcechess.ChessLogic;
import org.mxnik.forcechess.ChessLogic.Moves.MoveList;
import org.mxnik.forcechess.ChessLogic.Moves.MoveTypes;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenReader;
import org.mxnik.forcechess.ChessLogic.Notation.FenWriter;
import org.mxnik.forcechess.ChessLogic.Pieces.*;

import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.Util.FastBitmap;
import org.mxnik.forcechess.Util.Helper;

import java.util.Arrays;

import static org.mxnik.forcechess.ChessLogic.Notation.FenConversion.FromPiece;

public class Board {
    public static int sideLen = 8;
    public static int size = 64;

    Piece[] board;
    private boolean turn = true;
    int totalMaterial = 0;
    int maxDirs = 0;
    public int amountPieces;
    MoveList moveList;
    public int teamWMaterial;
    public int teamBMaterial;
    private int kingWPos;
    private int kingBPos;
    //globalIlegalMoves[x][0] ist die anzahl der Moves an dieser Stelle
    private static byte[][] globalLegalMoves;

    int maxMoves = 0;

    
    public Board(){
        board = new Piece[sideLen*sideLen];
        BuildFromFen("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        //muss nicht immer ein neues Array allocaten
        globalLegalMoves = new byte[Board.size][11];
    }

    public Board(String fenString){
        BuildFromFen(fenString);
        //muss nicht immer ein neues Array allocaten
        globalLegalMoves = new byte[Board.size][11];
    }
    /**
     * Board per Fen String aufbauen
     * @param fenStr Der String im Fen format
     * @throws FenException Exception mit position des Errors
     */
    public void BuildFromFen(String fenStr) throws FenException {
        // einfach callen nicht try catch (exception weitergeben)
        FenReader notation = new FenReader(fenStr);
        board = notation.readFenBoard();
        DiversePair<Integer, Integer> KingPositions = notation.readKingPos();
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

    private  void resetCastle(int from, int to) throws CloneNotSupportedException {
        if(board[from].getType() != PieceTypes.KING){
            return;
        }

        if(Helper.colDiff(from, to) > 1){

            // turm von original position immer noch in d
            int dir = Integer.compare(to, from);

            int rookPos = (dir < 0)?from - Helper.distanceLeftB(from):from + Helper.distanceRightB(from);
            rawMove(to - dir , rookPos, false);
        }
    }

    public GameState checkChess(byte[][] pseudelegalMoves) throws CloneNotSupportedException {
        boolean illegalMove = false;
        int movePtr;
        //TODO: find better way than copying entire arr
        Piece[] baseState = board.clone();
        int kB = kingBPos;
        int kW = kingWPos;
        boolean hasMoves = false;

        for (int i = 0; i < pseudelegalMoves.length; i++) {
            movePtr = 0;
            for (int j = 1; j < pseudelegalMoves[i][0]; j++) {
                byte move = pseudelegalMoves[i][j];


                //make move
                rawMove(i, move, false);

                if(turn){
                    illegalMove = isChecked(kingWPos);
                }else {
                    illegalMove = isChecked(kingBPos);
                }

                kingBPos = kB;
                kingWPos = kW;
                // TODO: find way to avoid copying entire array
                board = baseState.clone();

                if(illegalMove){
                   continue;
                }
                if(board[i].getColor() == turn){
                    hasMoves = true;
                }
                // move with no check for own king
                pseudelegalMoves[i][movePtr + 1] = move;
                movePtr ++;
            }
            pseudelegalMoves[i][0] = (byte) movePtr;
        }

        return checkCheckmate(hasMoves);
    }

    private GameState checkCheckmate(boolean hasMove) throws CloneNotSupportedException {
        boolean illegalMove;
        if(turn){
            illegalMove = isChecked(kingWPos);
        }else {
            illegalMove = isChecked(kingBPos);
        }

        if(hasMove){
            return GameState.Continue;
        }
        
        if(illegalMove){
            return GameState.CheckMate;
        }


        return GameState.StaleMate;
    }

    public boolean isChecked(int kingPos) throws CloneNotSupportedException {
        byte[][] moveFields = getPseudoMovesFromPosition();
        boolean check = false;

        fieldLoop:
        for (byte[] moves : moveFields){
            for (byte move : moves){
                if(move == kingPos){
                    check = true;
                    break fieldLoop;
                }
            }
        }

        return check;
    }

    public DiversePair<byte[][], GameState> getMovesFromPosition () throws CloneNotSupportedException{
        getPseudoMovesFromPosition();
        // modifies values in place
        return new DiversePair<>(globalLegalMoves, checkChess(globalLegalMoves));
    }

    /**
     * returned ein byte[Board.size][] array
     * Alle Moves werden mit position -> Moves gespeichert
     *
     * Unoptimiert aber funktionierend
     * @return byte[][] finalMoves
     */
    private byte[][] getPseudoMovesFromPosition() throws CloneNotSupportedException {


        moveList.clear();
        byte[] moves = moveList.getMovesArray();

        int pieceCount = 0;
        int prevMovecount = moveList.getMoveCount();

        for (int i = 0; i < board.length; i++) {
            // leere Felder skippen
            // könige Skippen ->
            // dannach berechnen um Schach moves zu verhindern
            if (board[i].getType() == PieceTypes.EMPTY) {
                globalLegalMoves[i][0] = 0;
                continue;
            }

            board[i].getMoves(i, moveList);

            //System.out.println("i: " + pieceCount +" piece: " +board[i]);
            int newMoveCount = moveList.getMoveCount();

            int dirStart = moveList.getDirectionOffset(pieceCount);
            int dirCount = moveList.getDirectionCount(pieceCount);

            // keep track of the current position in the final moves arr
            int ptr = 0;
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
                                Board.globalLegalMoves[i][ptr + j + 1] = square;
                                ptr++;
                            }
                            break;
                        } else{
                            if (board[square] != EmptyPiece.EMPTY_PIECE) {
                                break;
                            }
                        }
                    }else if(board[i].getType() == PieceTypes.KING){
                        int dir = Integer.compare(square, i);
                        Piece corner = (dir < 0)?board[i - Helper.distanceLeftB(i)]:board[i + Helper.distanceRightB(i)];
                        // hideous
                        if(Helper.colDiff(i, square) > 1) {
                            if (board[i].isHasMoved() || corner.isHasMoved() || corner.getType()!=PieceTypes.ROOK) {
                                break;
                            }

                            // can't be out of bounds because the move won't be registered
                            for(int k = i+dir; k != square; k+=dir){
                                if(board[k] != EmptyPiece.EMPTY_PIECE){
                                    break directionLoop;
                                }
                            }
                            Board.globalLegalMoves[i][ptr + j + 1] = square;
                            ptr++;
                            break;
                        }
                    }
                    // blocked → stop this direction
                    if (board[square] != EmptyPiece.EMPTY_PIECE) {
                        // different color -> take piece: else don't
                        if(board[square].getColor() != board[i].getColor()) {
                            Board.globalLegalMoves[i][ptr + j + 1] = square;
                            ptr++;
                        }
                        break;
                    }

                    Board.globalLegalMoves[i][ptr + j + 1] = square;
                    //System.out.printf("legalMove: %d -> %d\n", i, square);
                    //otherwise square is free → legal move
                }
                ptr += j;
            }
            globalLegalMoves[i][0] = (byte) ptr;
            pieceCount ++;
        }


        return globalLegalMoves;
    }

    private MoveTypes castleFreeMove(int from, int to, boolean moved) throws CloneNotSupportedException {
        if(board[from] == EmptyPiece.EMPTY_PIECE){
            return MoveTypes.KingMove;
        }
        if(board[to] != EmptyPiece.EMPTY_PIECE){
            amountPieces -= 1;
        }
        Piece p = board[from].clone();
        p.setHasMoved(moved);
        board[from] = EmptyPiece.EMPTY_PIECE;
        board[to] = p;


        if (p.getType() != PieceTypes.KING){
            return MoveTypes.GoodMove;
        }

        return MoveTypes.KingMove;
    }

    private MoveTypes rawMove(int from, int to, boolean moved) throws CloneNotSupportedException {

        // no king move
        if (castleFreeMove(from, to, moved) != MoveTypes.KingMove){
            return MoveTypes.GoodMove;
        }

        if(turn){
            kingWPos = to;
        }else {
            kingBPos = to;
        }


        if(Helper.colDiff(from, to) > 1){
            int dir = Integer.compare(to, from);
            int rookPos = (dir < 0)?from - Helper.distanceLeftB(from):from + Helper.distanceRightB(from);
            rawMove(rookPos, to - dir, moved);
            return MoveTypes.Castle;
        }

        return MoveTypes.KingMove;
    }

    public void move(int from , int to) throws CloneNotSupportedException {
        rawMove(from, to, true);
        turn = !turn;
    }

    /**
     * Gibt das Board in einem String zurück der ein Board in textform darstellt
     */
    public String toStringBoard() {
        StringBuilder sb = new StringBuilder();
        int cellWidth = 3;
        String horizontalLine = "+" + ("---+").repeat(sideLen) + "\n";

        for (int rank = sideLen - 1; rank >= 0; rank--) {
            sb.append(horizontalLine);
            sb.append("|");
            for (int file = 0; file < sideLen; file++) {
                int index = rank * sideLen + file;
                Piece piece = board[index];

                char symbol;
                if (piece == null || piece.getType() == PieceTypes.EMPTY) {
                    symbol = ' ';
                } else {
                    try {
                        symbol = FromPiece(piece.getType(), piece.getColor());
                    } catch (FenException e) {
                        symbol = '?';
                    }
                }
                sb.append(" ").append(symbol).append(" |");
            }
            sb.append(" ").append(rank + 1).append("\n");
        }

        sb.append(horizontalLine);
        sb.append(" ");
        for (int file = 0; file < sideLen; file++) {
            sb.append(" ").append((char) ('a' + file)).append("  ");
        }
        sb.append("\n");

        return sb.toString();

        /*

    This will render like:
            ```
            +---+---+---+---+---+---+---+---+
            | r | n | b | q | k | b | n | r | 8
            +---+---+---+---+---+---+---+---+
            | p | p | p | p | p | p | p | p | 7
            +---+---+---+---+---+---+---+---+
            |   |   |   |   |   |   |   |   | 6
            +---+---+---+---+---+---+---+---+
            |   |   |   |   |   |   |   |   | 5
            +---+---+---+---+---+---+---+---+
            |   |   |   |   |   |   |   |   | 4
            +---+---+---+---+---+---+---+---+
            |   |   |   |   |   |   |   |   | 3
            +---+---+---+---+---+---+---+---+
            | P | P | P | P | P | P | P | P | 2
            +---+---+---+---+---+---+---+---+
            | R | N | B | Q | K | B | N | R | 1
            +---+---+---+---+---+---+---+---+
              a   b   c   d   e   f   g   h

 */
    }



    /**
     * Gibt, das board als Fen String aus
     * @return fenString
     */
    public String WriteAsFen(){
        return FenWriter.WriteFen(this);
    }


    public Piece[] getBoard() {
        return board;
    }

    public void setBoard(Piece[] board) {
        this.board = board;
    }

    public boolean getTurn(){
        return turn;
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        Board board1 = new Board("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        long starT = System.nanoTime();
        byte[][] allMoves = null;
        for (int i = 0; i < 1000000; i++) {
            allMoves = board1.getMovesFromPosition().first();
        }
        long endT = System.nanoTime();
        long timeT = endT - starT;


        System.out.println("-----------------------");

        System.out.printf("""
                        took time for full 1000000: %d s
                        avg time per board: %d Us
                        so on avg %d per sec
                        """,
                timeT / 100000000,
                (timeT / 1000000) / 1000,
                100000000 / (timeT / 1000000));

        System.out.println("-----------------------");
        /*
        stand 13.03.2026 - kein Schach check
        -----------------------
        took time for full 1000000: 1687212500ns
        avg time per board: 1687ns
        so on avg 59276 per sec
        -----------------------


        stand 15.03.2026 - kein Schach check

        -----------------------
        took time for full 1000000: 82226015600 ns
        avg time per board: 82226 ns
        so on avg 1216 per sec
        -----------------------
         */

    }
}