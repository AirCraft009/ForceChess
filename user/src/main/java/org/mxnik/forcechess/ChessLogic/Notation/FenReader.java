package org.mxnik.forcechess.ChessLogic.Notation;

import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.General.FenException;

import java.util.Arrays;

import static org.mxnik.forcechess.Pos.Piece.*;
import static org.mxnik.forcechess.Pos.Piece.BISHOP;
import static org.mxnik.forcechess.Pos.Piece.KING;
import static org.mxnik.forcechess.Pos.Piece.KNIGHT;
import static org.mxnik.forcechess.Pos.Piece.PAWN;
import static org.mxnik.forcechess.Pos.Piece.QUEEN;
import static org.mxnik.forcechess.Pos.Piece.ROOK;
import static org.mxnik.forcechess.Pos.PositionEncoder.SIZE;


public final class FenReader {
    // sollte hidden sein
    // Board API sollte readFenBoard and writeFen exposen
    private final String boardPositions;
    private final String turn;
    private final String castle;
    private final String enPassent;
    private final String moveNumber;
    private final int boardLenght;
    private int kingWPos;
    private int kingBPos;

    public FenReader(String fenStr){
        String [] fenParts = fenStr.split(" ");
        if (fenParts.length != 6) {
            throw new FenException("incomplete Fenstring incorrect number of subsections, expected 6 got: " + fenParts.length, 0);
        }
        boardPositions = fenParts[0];
        turn = fenParts[1];
        castle = fenParts[2];
        enPassent = fenParts[3];
        moveNumber = fenParts[4];
        boardLenght = Integer.parseInt(fenParts[5]);
    }

    public DiversePair<Integer, Integer> readKingPos(){
        return new DiversePair<>(kingWPos, kingBPos);
    }

    public int getBoardLenght(){
        return boardLenght;
    }


    public boolean readFenTurn() throws FenException {
        return turn.equals("w");
    }

    public int readSideLen() {
        return boardLenght;
    }

    public int readEnpassent(){
        if(enPassent.equals("-")){
            return -1;
        }
        if(enPassent.length() != 2){
            throw new FenException("En Passant Square illegaly isn't 4 chars long but: " + enPassent.length(), -1);
        }

        String sq = enPassent.substring(0,2).toLowerCase();

        int col = sq.charAt(0) - 'a';
        int row = (sq.charAt(1) - '0') - 1;

        return col + row * 8;

    }

    public static String toFieldName(int field){
        int row = field / SIZE;
        int col = field % SIZE;

        return  Character.toString('a' + col) + row;
    }

    /**
     * Reads a fenstr and turns it into a board
     * This isn't a normal Fen string information is in the specification markdown
     * @return a board of pieces
     * @throws FenException exception with the position
     */
    public Piece[] readFenBoard() throws FenException {
        int boardSize = boardLenght * boardLenght;
        Piece[] board = new Piece[boardSize];
        Arrays.fill(board, EmptyPiece.EMPTY_PIECE);

        int charptr = 0;
        int skip = 0;

        outerloop:
        for (int i = boardLenght - 1; i >= 0; i--) {
            for (int j = 0; j < boardLenght; j++) {
                int square = i * boardLenght + j;
                char p = boardPositions.charAt(charptr);
                charptr ++;
                while (Character.isDigit(p)){
                    // one will be added in the next loop
                    skip *= 10;
                    skip += (p - '0');
                    if(charptr >= boardPositions.length())
                        break outerloop;
                    p = boardPositions.charAt(charptr);
                    charptr++;
                }
                if(skip != 0){
                    j += skip - 1;
                    if(j > boardLenght)
                        throw  new FenException("Gap between two pieces too big: " + j, square);
                    skip = 0;
                    charptr--;
                    continue;
                }

                if (p == '/') {      // new row
                    j--;
                    continue;
                }

                Piece piece = FenConversion.FromFen(p);
                if (piece.getType() == PieceTypes.ILLEGAL) {
                    throw new FenException("Illegal char found in fenStr: " + p, charptr);
                } else if (piece.getType() == PieceTypes.KING){
                    if (piece.getColor()){
                        kingWPos = square;
                    }else {
                        kingBPos = square;
                    }
                }

                board[square] = piece;
            }
        }

        return board;
    }

    public static void main(String[] args) {
        //System.out.println(Arrays.toString(("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", 8)));
    }
}
