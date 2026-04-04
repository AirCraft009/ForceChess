package org.mxnik.forcechess.user.ChessLogic;
import org.mxnik.forcechess.user.ChessLogic.Moves.MoveList;
import org.mxnik.forcechess.user.ChessLogic.Moves.MoveOffsets;
import org.mxnik.forcechess.user.ChessLogic.Moves.MoveTypes;
import org.mxnik.forcechess.Util.Notation.FenException;
import org.mxnik.forcechess.Util.Notation.FenReader;
import org.mxnik.forcechess.Util.Notation.FenWriter;

import org.mxnik.forcechess.user.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.user.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.user.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.Util.Helper;

import java.util.Arrays;

import static org.mxnik.forcechess.Util.Notation.FenConversion.FromPiece;
import static org.mxnik.forcechess.Util.RayDetection.*;

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
    private int enPassantPos;

    int maxMoves = 0;

    public Board() {
        board = new Piece[sideLen * sideLen];
        BuildFromFen("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
    }

    public Board(String fenString) {
        BuildFromFen(fenString);
        MoveOffsets.calculateOffset(sideLen);
    }

    /**
     * Board per Fen String aufbauen
     * @param fenStr Der String im Fen format
     * @throws FenException Exception mit position des Errors
     */
    public void BuildFromFen(String fenStr) throws FenException {
        FenReader notation = new FenReader(fenStr);
        board = notation.readFenBoard();
        DiversePair<Integer, Integer> KingPositions = notation.readKingPos();
        kingWPos = KingPositions.first();
        kingBPos = KingPositions.second();
        turn = notation.readFenTurn();
        sideLen = notation.readSideLen();
        MoveOffsets.calculateOffset(sideLen);
        Piece.refreshMoveSets();

        for (int i = 0; i < board.length; i++) {
            Piece p = board[i];
            if (p == EmptyPiece.EMPTY_PIECE)
                continue;

            totalMaterial += p.getType().value;
            amountPieces++;
            maxDirs += p.getMaxDir();
            maxMoves += p.getMovesetLen();
        }
        Board.size = board.length;
        moveList = new MoveList(amountPieces, maxDirs, maxMoves);
    }



    /**
     * Ist König in Schach.
     *
     * Verwendet Ray-Modell beginnt vom könig aus in alle Richtungen (slide) und Springerform
     * und check für Schach.
     *
     * @param kingPos   flat board index of the king to test
     * @param kingColor true = white king, false = black king
     */
    public boolean isChecked(int kingPos, boolean kingColor) {
        int kingRow = Helper.getRow(kingPos);
        int kingCol = Helper.getCol(kingPos);

        //  Rays (straight + diagonal)
        for (int d = 0; d < 8; d++) {
            int dr = RAY_ROW[d];
            int dc = RAY_COL[d];
            int r = kingRow + dr;
            int c = kingCol + dc;
            boolean firstStep = true;

            while (r >= 0 && r < sideLen && c >= 0 && c < sideLen) {
                Piece p = board[r * sideLen + c];

                if (p != EmptyPiece.EMPTY_PIECE) {
                    if (p.getColor() != kingColor) {
                        PieceTypes t = p.getType();

                        if (d < 4) {
                            // Straight ray: rook or queen attacks
                            if (t == PieceTypes.ROOK || t == PieceTypes.QUEEN) return true;
                        } else {
                            // Diagonal ray: bishop or queen attacks
                            if (t == PieceTypes.BISHOP || t == PieceTypes.QUEEN) return true;

                            // Pawn only attacks on the first diagonal step.
                            // A white pawn sits below and attacks upward (r < kingRow).
                            // A black pawn sits above and attacks downward (r > kingRow).
                            if (firstStep && t == PieceTypes.PAWN) {
                                boolean pawnIsWhite = p.getColor();
                                if (pawnIsWhite && r < kingRow) return true;
                                if (!pawnIsWhite && r > kingRow) return true;
                            }
                        }
                    }
                    // Blocker found (friend or foe) — ray is cut off
                    break;
                }

                r += dr;
                c += dc;
                firstStep = false;
            }
        }

        //  Knight jumps
        for (int k = 0; k < 8; k++) {
            int r = kingRow + KNIGHT_ROW[k];
            int c = kingCol + KNIGHT_COL[k];
            if (r < 0 || r >= sideLen || c < 0 || c >= sideLen) continue;

            Piece p = board[r * sideLen + c];
            if (p != EmptyPiece.EMPTY_PIECE
                    && p.getColor() != kingColor
                    && p.getType() == PieceTypes.KNIGHT) {
                return true;
            }
        }

        //  Adjacent enemy king (prevents kings walking next to each other)
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int r = kingRow + dr;
                int c = kingCol + dc;
                if (r < 0 || r >= sideLen || c < 0 || c >= sideLen) continue;

                Piece p = board[r * sideLen + c];
                if (p != EmptyPiece.EMPTY_PIECE
                        && p.getColor() != kingColor
                        && p.getType() == PieceTypes.KING) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Filters pseudo-legal moves down to legal moves by making each move,
     * running incremental check detection, then restoring the board.
     * Modifies pseudoLegalMoves in place and returns the resulting game state.
     */
    public GameState checkChess(byte[][] pseudoLegalMoves) throws CloneNotSupportedException {
        Piece[] baseState = board.clone();
        int kB = kingBPos;
        int kW = kingWPos;
        boolean hasMoves = false;

        for (int i = 0; i < pseudoLegalMoves.length; i++) {
            byte[] moves = pseudoLegalMoves[i];
            int writePtr = 0;

            for (int j = 0; j < moves.length; j++) {
                byte move = moves[j];

                rawMove(i, move, false);

                // isChecked now uses incremental ray casting instead of full
                // move generation — the hot path is now ~80 ops instead of ~300+
                boolean inCheck = turn
                        ? isChecked(kingWPos, true)
                        : isChecked(kingBPos, false);

                kingBPos = kB;
                kingWPos = kW;
                board = baseState.clone();

                if (inCheck) continue;

                if (board[i].getColor() == turn) {
                    hasMoves = true;
                }

                moves[writePtr] = move;
                writePtr++;
            }

            pseudoLegalMoves[i] = Arrays.copyOf(moves, writePtr);
        }

        return checkCheckmate(hasMoves);
    }

    private GameState checkCheckmate(boolean hasMove) throws CloneNotSupportedException {
        if (hasMove) {
            return GameState.Continue;
        }

        // No moves: distinguish checkmate from stalemate
        boolean inCheck = turn
                ? isChecked(kingWPos, true)
                : isChecked(kingBPos, false);

        return inCheck ? GameState.CheckMate : GameState.StaleMate;
    }

    public DiversePair<byte[][], GameState> getMovesFromPosition() throws CloneNotSupportedException {
        byte[][] legalMoves = getPseudoMovesFromPosition();
        return new DiversePair<>(legalMoves, checkChess(legalMoves));
    }

    /**
     * Generates pseudo-legal moves for all squares.
     * Does not filter for leaving own king in check — that is done in checkChess.
     */
    private byte[][] getPseudoMovesFromPosition() throws CloneNotSupportedException {
        moveList.clear();
        byte[] moves = moveList.getMovesArray();

        byte[][] legalMoves = new byte[size][];

        int pieceCount = 0;
        int prevMoveCount = moveList.getMoveCount();

        for (int i = 0; i < board.length; i++) {
            if (board[i].getType() == PieceTypes.EMPTY) {
                legalMoves[i] = new byte[0];
                continue;
            }

            board[i].getMoves(i, moveList);

            int newMoveCount = moveList.getMoveCount();
            int dirStart = moveList.getDirectionOffset(pieceCount);
            int dirCount = moveList.getDirectionCount(pieceCount);

            int ptr = 0;
            byte[] legalMoveSection = new byte[newMoveCount - prevMoveCount];
            prevMoveCount = newMoveCount;

            for (int d = 0; d < dirCount; d++) {
                int dirIndex = dirStart + d;
                int moveOffset = moveList.getDirectionMovesOffset(dirIndex);
                int moveLength = moveList.getDirectionMovesLength(dirIndex);

                int j;
                moveLoop:
                for (j = 0; j < moveLength; j++) {
                    byte square = moves[moveOffset + j];

                    if (board[i].getType() == PieceTypes.PAWN) {
                        if (Helper.isDiagonalMove(i, square)) {
                            if ((board[square].getColor() != board[i].getColor()
                                    && board[square] != EmptyPiece.EMPTY_PIECE)
                                    || square == enPassantPos) {
                                legalMoveSection[ptr] = square;
                                ptr++;
                            }
                            break;
                        } else {
                            if (board[square] != EmptyPiece.EMPTY_PIECE) {
                                break;
                            }
                        }
                    } else if (board[i].getType() == PieceTypes.KING) {
                        int dir = Integer.compare(square, i);
                        int cornerPos = (dir < 0)
                                ? i - Helper.distanceLeftB(i)
                                : i + Helper.distanceRightB(i);
                        Piece corner = board[cornerPos];

                        if (Helper.colDiff(i, square) > 1) {
                            if (board[i].isHasMoved()
                                    || corner.isHasMoved()
                                    || corner.getType() != PieceTypes.ROOK) {
                                break;
                            }
                            for (int k = i + dir; k != cornerPos; k += dir) {
                                if (board[k] != EmptyPiece.EMPTY_PIECE) {
                                    break moveLoop;
                                }
                            }
                            legalMoveSection[ptr] = square;
                            ptr++;
                            break;
                        }
                    }

                    if (board[square] != EmptyPiece.EMPTY_PIECE) {
                        if (board[square].getColor() != board[i].getColor()) {
                            legalMoveSection[ptr] = square;
                            ptr++;
                        }
                        break;
                    }

                    legalMoveSection[ptr] = square;
                    ptr++;
                }
            }

            legalMoves[i] = Arrays.copyOf(legalMoveSection, ptr);
            pieceCount++;
        }

        return legalMoves;
    }

    private MoveTypes castleFreeMove(int from, int to, boolean moved) throws CloneNotSupportedException {
        if (board[from] == EmptyPiece.EMPTY_PIECE) {
            return MoveTypes.KingMove;
        }
        if(moved) {
            int pawnDir = (board[from].getColor())? 1:-1;
            if(board[from].getType() == PieceTypes.PAWN && Helper.isDiagonalMove(from, to) && to == enPassantPos){
                //En Passant take
                board[to + MoveOffsets.DOWN.offset*pawnDir] = EmptyPiece.EMPTY_PIECE;
                amountPieces -= 1;
            }
            if (board[from].getType() == PieceTypes.PAWN
                    && Helper.rowDiff(from, to) > 1) {
                //En Passant will be possible next move
                enPassantPos = from + MoveOffsets.UP.offset*pawnDir;
                //System.out.println(enPassantPos);
            } else {
                //En Passant no longer possible
                enPassantPos = -1;
            }
        }
        if (board[to] != EmptyPiece.EMPTY_PIECE) {
            amountPieces -= 1;
        }
        Piece p = board[from].clone();
        p.setHasMoved(moved);
        board[from] = EmptyPiece.EMPTY_PIECE;
        board[to] = p;

        if (p.getType() != PieceTypes.KING) {
            return MoveTypes.GoodMove;
        }
        return MoveTypes.KingMove;
    }

    private MoveTypes rawMove(int from, int to, boolean moved) throws CloneNotSupportedException {
        if (castleFreeMove(from, to, moved) != MoveTypes.KingMove) {
            return MoveTypes.GoodMove;
        }

        if (turn) {
            kingWPos = to;
        } else {
            kingBPos = to;
        }

        if (Helper.colDiff(from, to) > 1) {
            int dir = Integer.compare(to, from);
            int rookPos = (dir < 0)
                    ? from - Helper.distanceLeftB(from)
                    : from + Helper.distanceRightB(from);
            rawMove(rookPos, to - dir, moved);
            return MoveTypes.Castle;
        }

        return MoveTypes.KingMove;
    }

    public void move(int from, int to) throws CloneNotSupportedException {
        rawMove(from, to, true);
        turn = !turn;
    }

    /**
     * Gibt das Board in einem String zurück der ein Board in textform darstellt
     */
    public String toStringBoard() {
        StringBuilder sb = new StringBuilder();
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
    }

    /**
     * Gibt das board als Fen String aus
     * @return fenString
     */
    public String WriteAsFen() {
        return FenWriter.WriteFen(this);
    }

    public Piece[] getBoard() {
        return board;
    }

    public void setBoard(Piece[] board) {
        this.board = board;
    }

    public boolean getTurn() {
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

        System.out.println("----------------------");
        System.out.printf("took time for full 1000000: %d ns\n" +
                        "avg time per board: %d ns\n" +
                        "so on avg %d per sec\n",
                timeT,
                timeT / 1000000,
                1000000000L / (timeT / 1000000));
        System.out.println("----------------------");
    }
}
