package org.mxnik.forcechess.Util.Notation;

import org.mxnik.forcechess.user.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.user.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.user.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.Util.DiversePair;

import java.util.Arrays;


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
        try {
            boardLenght = Integer.parseInt(fenParts[5]);
        } catch (NumberFormatException e) {
            throw new FenException("last FenString component wasn't a number(board-length), expected num got: " + fenParts[5], 0);
        }
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

    public boolean[] readCastleRights(){
        return new boolean[4];
    }

    public int readEnpassent(){
        try {
            return  Integer.parseInt(enPassent);
        } catch (NumberFormatException e) {
            throw new FenException("last FenString component wasn't a number(en-passant square), expected num got: " + enPassent, 0);
        }
    }

    /**
     * Reads a fenstr and turns it into a board
     * This isn't a normal Fen string information is in the specification markdown
     * @return a board of pieces
     * @throws FenException exception with the position
     */
    public Piece[] readFenBoard() throws FenException {
        int boardSize = boardLenght * boardLenght;
        char[] chars = boardPositions.toCharArray();
        Piece[] board = new Piece[boardSize];
        Arrays.fill(board, EmptyPiece.EMPTY_PIECE);

        int ptr = boardSize;
        int charptr = 0;
        int fieldsinRow = 0;
        int factor = 1;

        for (int i = 0; i < boardLenght; i++) {
            ptr -= boardLenght;
            for (int j = 0; j < boardLenght; j++) {
                char c = chars[charptr];
                charptr++;
                if (c == '/') {
                    factor = 1;
                    j--;
                    continue;
                }

                if (Character.isDigit(c)) {
                    if (factor > 10) {
                        throw new FenException("Skip number bigger than 99", charptr);
                    }
                    int skip = (c - '0') * factor;
                    if (j + skip - 1 > boardLenght)
                        throw new FenException("Illegal fenStr, cannot skip more than one row: " + boardPositions, charptr);
                    j += skip - 1;
                    factor *= 10;
                    continue;
                }

                Piece p = FenConversion.FromFen(c);
                factor = 1;
                if (p.getType() == PieceTypes.ILLEGAL) {
                    throw new FenException("Illegal char found in fenStr: " + c, charptr);
                } else if (p.getType() == PieceTypes.KING){
                    if (p.getColor()){
                        kingWPos = ptr + j;
                    }else {
                        kingBPos = ptr + j;
                    }
                }

                board[ptr + j] = p;
            }
        }

        if (ptr != 0) {
            throw new FenException("Fen isn't complete: ", ptr);
        }
        return board;
    }

    public static void main(String[] args) {
        //System.out.println(Arrays.toString(("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", 8)));
    }
}
