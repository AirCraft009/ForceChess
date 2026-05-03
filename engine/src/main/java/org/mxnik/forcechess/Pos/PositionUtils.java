package org.mxnik.forcechess.Pos;

import org.mxnik.forcechess.Bitboard;
import org.mxnik.forcechess.FenException;

import java.util.Arrays;
import java.util.Random;

import static org.mxnik.forcechess.Pos.Move.ROW_1;
import static org.mxnik.forcechess.Pos.Piece.*;
import static org.mxnik.forcechess.Pos.PositionEncoder.Position.*;

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
        var pos = new PositionEncoder.Position();

        int parsePos = 0;

        for (int i = 0; i < PositionEncoder.SIZE; i++) {
            for (int j = 0; j < PositionEncoder.SIZE; j++) {
                int square = i * PositionEncoder.SIZE + j;
                char p = pieces.charAt(parsePos);
                parsePos ++;
                if(Character.isDigit(p)){
                    j += p - '0';
                    if(j > PositionEncoder.SIZE)
                        throw  new FenException("Gap between two pieces to big (more than 8 in sum): " + j, square);
                    continue;
                } else if (p == '/') {      // new row
                    j--;
                    continue;
                }

                switch (p){
                    case 'p':
                        place(pos, false, PAWN, square);;

                    case 'n':
                        place(pos, false, KNIGHT, square);;

                    case 'b':
                        place(pos, false, BISHOP, square);;

                    case 'r':
                        place(pos, false, ROOK, square);;

                    case 'q':
                        place(pos, false, QUEEN, square);;

                    case 'k':
                        place(pos, false, KING, square);;

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
            pos.fiftyMoveCounter = Integer.parseInt(parts[4]) * 2;
        }catch (NumberFormatException e){
            throw new FenException("Error when reading move number", -1);
        }
        return pos;
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

        return row * PositionEncoder.SIZE + col;
    }

    public static void main(String[] args) {
        var pos = fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        System.out.println(Bitboard.visualiseBitboard(pos.Occupied));
    }
}
