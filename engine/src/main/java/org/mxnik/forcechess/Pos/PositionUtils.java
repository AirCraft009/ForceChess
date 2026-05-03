package org.mxnik.forcechess.Pos;

import org.mxnik.forcechess.Bitboard;
import org.mxnik.forcechess.FenException;

import java.util.Arrays;
import java.util.Random;

import static org.mxnik.forcechess.Pos.Move.ROW_1;
import static org.mxnik.forcechess.Pos.Piece.*;
import static org.mxnik.forcechess.Pos.PositionEncoder.Position.*;
import static org.mxnik.forcechess.Pos.PositionEncoder.SIZE;

public class PositionUtils {

    /** Shallow clone used in legal-move filter tests. */
    public static PositionEncoder.Position deepCopy(PositionEncoder.Position src) {
        var d = new PositionEncoder.Position();
        d.WPawns = src.WPawns;     d.WKnights = src.WKnights;
        d.WBishops = src.WBishops; d.WRooks   = src.WRooks;
        d.WQueens  = src.WQueens;  d.WKing    = src.WKing;
        d.BPawns   = src.BPawns;   d.BKnights = src.BKnights;
        d.BBishops = src.BBishops; d.BRooks   = src.BRooks;
        d.BQueens  = src.BQueens;  d.BKing    = src.BKing;
        d.Occupied = src.Occupied; d.WPieces  = src.WPieces;
        d.BPieces  = src.BPieces;
        d.WQueenCastle = src.WQueenCastle; d.WKingCastle = src.WKingCastle;
        d.BQueenCastle = src.BQueenCastle; d.BKingCastle = src.BKingCastle;
        d.enPassantSquare  = src.enPassantSquare;
        d.whiteToMove      = src.whiteToMove;
        d.fiftyMoveCounter = src.fiftyMoveCounter;
        d.pieceMap = Arrays.copyOf(src.pieceMap, 64);
        return d;
    }

    /**
     * Place a piece and keep pieceMap + bitboards + aggregate boards in sync.
     *   color : true = white, false = black
     *   type  : Piece.PAWN / KNIGHT / BISHOP / ROOK / QUEEN / KING
     */
    public static void place(PositionEncoder.Position pos, boolean color, int type, int sq) {
        // Piece.of(color, type) must produce the byte stored in pieceMap.
        int piece = Piece.of(color, type);
        pos.pieceMap[sq] = (byte) piece;

        long bit = Bitboard.set(0L, sq);
        if (color) {
            switch (type) {
                case PAWN   -> pos.WPawns   |= bit;
                case Piece.KNIGHT -> pos.WKnights |= bit;
                case Piece.BISHOP -> pos.WBishops |= bit;
                case Piece.ROOK   -> pos.WRooks   |= bit;
                case Piece.QUEEN  -> pos.WQueens  |= bit;
                case Piece.KING   -> pos.WKing    |= bit;
            }
            pos.WPieces |= bit;
        } else {
            switch (type) {
                case PAWN   -> pos.BPawns   |= bit;
                case Piece.KNIGHT -> pos.BKnights |= bit;
                case Piece.BISHOP -> pos.BBishops |= bit;
                case Piece.ROOK   -> pos.BRooks   |= bit;
                case Piece.QUEEN  -> pos.BQueens  |= bit;
                case Piece.KING   -> pos.BKing    |= bit;
            }
            pos.BPieces |= bit;
        }
        pos.Occupied |= bit;
    }

    public static PositionEncoder.Position fromFen(String fenStr){
        String[] parts = fenStr.split(" ");
        if(parts.length != 6)
            throw new FenException("illegal number of parts in fen string", -1);

        String pieces = parts[0];
        var pos = PositionEncoder.Position.emptyPosition();

        int parsePos = 0;

        for (int i = SIZE - 1; i >= 0; i--) {
            for (int j = 0; j < SIZE; j++) {
                int square = i * SIZE + j;
                char p = pieces.charAt(parsePos);
                parsePos ++;
                if(Character.isDigit(p)){
                    j += p - '0';
                    if(j > SIZE)
                        throw  new FenException("Gap between two pieces to big (more than 8 in sum): " + j, square);
                    continue;
                } else if (p == '/') {      // new row
                    j--;
                    continue;
                }

                switch (p){
                    case 'p':
                        place(pos, false, PAWN, square);
                        break;

                    case 'n':
                        place(pos, false, KNIGHT, square);
                        break;
                    case 'b':
                        place(pos, false, BISHOP, square);
                        break;
                    case 'r':
                        place(pos, false, ROOK, square);
                        break;
                    case 'q':
                        place(pos, false, QUEEN, square);
                        break;
                    case 'k':
                        place(pos, false, KING, square);
                        break;
                    case 'P':
                        place(pos, true, PAWN, square);
                        break;

                    case 'N':
                        place(pos, true, KNIGHT, square);
                        break;

                    case 'B':
                        place(pos, true, BISHOP, square);
                        break;

                    case 'R':
                        place(pos, true, ROOK, square);
                        break;

                    case 'Q':
                        place(pos, true, QUEEN, square);
                        break;

                    case 'K':
                        place(pos, true, KING, square);
                        break;
                    default:
                        throw new FenException("Illegal char: " + p, square);
                }
            }
        }

        // basic setup
        pos.WDoublePawnMove = 0x000000000000FF00L;
        pos.BDoublePawnMove = 0x00FF000000000000L;
        pos.calcMat();

        // side to move
        pos.whiteToMove = parts[1].equals("w");
        // castle permissions
        for (int i = 0; i < parts[2].length(); i++) {
            char right = parts[2].charAt(i);
            pos.castlePerms |= switch (right) {
                case 'K' -> W_KINGSIDE;
                case 'Q' -> W_QUEENSIDE;
                case 'k' -> B_KINGSIDE;
                case 'q' -> B_QUEENSIDE;
                case '-' -> 0;              // no perms
                default -> throw new FenException("Illegal char in Castle-perms.", i);
            };
        }
        // En passant
        try {
            if(!parts[3].equals("-"))
                pos.enPassantSquare = parseFieldName(parts[3]);
            else
                pos.enPassantSquare = -1;

        } catch (IllegalArgumentException e) {
            throw new FenException("Error while parsing the enPassant square: longer than two chars", -1);
        }


        // Move number
        try {
            pos.fiftyMoveCounter = Integer.parseInt(parts[4]);
        }catch (NumberFormatException e){
            throw new FenException("Error when reading move number", -1);
        }
        return pos;
    }

    public static String toFen(PositionEncoder.Position pos){
        StringBuilder fenBuilder = new StringBuilder();
        int skip = 0;

        for (int i = SIZE - 1; i >= 0 ; i--) {
            for (int j = 0; j < SIZE; j++) {
                int square = i * SIZE + j;
                if (pos.pieceMap[square] == EMPTY_PIECE){
                    skip ++;
                    continue;
                }
                if(skip > 0){
                    fenBuilder.append(skip);
                    skip = 0;
                }

                if(Piece.color(pos.pieceMap[square])) {
                    switch (Piece.pieceT(pos.pieceMap[square])) {
                        case PAWN -> fenBuilder.append('P');
                        case KNIGHT -> fenBuilder.append('N');
                        case BISHOP -> fenBuilder.append('B');
                        case ROOK -> fenBuilder.append('R');
                        case QUEEN -> fenBuilder.append('Q');
                        case KING -> fenBuilder.append('K');
                    }
                }else {
                    switch (Piece.pieceT(pos.pieceMap[square])) {
                        case PAWN -> fenBuilder.append('p');
                        case KNIGHT -> fenBuilder.append('n');
                        case BISHOP -> fenBuilder.append('b');
                        case ROOK -> fenBuilder.append('r');
                        case QUEEN -> fenBuilder.append('q');
                        case KING -> fenBuilder.append('k');
                    }
                }

            }
            if (skip > 0){
                fenBuilder.append(skip);
                skip = 0;
            }
            if(i != 0)
                fenBuilder.append("/");
        }

        fenBuilder.append(" ");

        // side to move
        fenBuilder.append((pos.whiteToMove)? "w " : "b ");


        // write up the castle permissions
        if(pos.castlePerms != 0){
            if(pos.queryCastlePerms(W_KINGSIDE))
                fenBuilder.append("K");
            if(pos.queryCastlePerms(W_QUEENSIDE))
                fenBuilder.append("Q");
            if(pos.queryCastlePerms(B_KINGSIDE))
                fenBuilder.append("k");
            if(pos.queryCastlePerms(B_QUEENSIDE))
                fenBuilder.append("q");
        }else {
            fenBuilder.append("-");
        }

        fenBuilder.append(" ");

        //en passant
        if(pos.enPassantSquare != -1){
            fenBuilder.append(toFieldName(pos.enPassantSquare));
        }else {
            fenBuilder.append("-");
        }

        fenBuilder.append(" ");

        // fifty move counter
        fenBuilder.append(pos.fiftyMoveCounter).append(" ");

        // temporarily write 1 only as this isn't important to us
        fenBuilder.append("1");
        return fenBuilder.toString();
    }

    /**
     * parses a field like A6 and returns the integer representation
     */
    public static int parseFieldName(String fieldName) throws IllegalArgumentException {
        if(fieldName.length() > 2){
            throw new IllegalArgumentException("A chessField can only be two chars long Column Row");
        }
        fieldName = fieldName.toLowerCase();

        int col = fieldName.charAt(0) - 'a';
        int row = fieldName.charAt(1) - '0';

        return row * SIZE + col;
    }

    public static String toFieldName(int field){
        int row = field / SIZE;
        int col = field % SIZE;

        return  Character.toString('a' + col) + row;
    }

    public static void main(String[] args) {
        var pos = fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
        System.out.println(toFen(pos));
    }
}
