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

    // 1 length byte + 27 max moves (queen on open board)
    // increase if you ever support fairy pieces with longer rays
    static final int MOVE_STRIDE = 28;

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

    // flat stride array: square i -> base = i * MOVE_STRIDE
    //   [base]        = number of legal moves for that square
    //   [base + 1..n] = the move targets
    private static byte[] globalLegalMoves;

    int maxMoves = 0;

    public Board() {
        board = new Piece[sideLen * sideLen];
        BuildFromFen("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        globalLegalMoves = new byte[Board.size * MOVE_STRIDE];
    }

    public Board(String fenString) {
        BuildFromFen(fenString);
        globalLegalMoves = new byte[Board.size * MOVE_STRIDE];
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

    private void resetCastle(int from, int to) throws CloneNotSupportedException {
        if (board[from].getType() != PieceTypes.KING) {
            return;
        }

        if (Helper.colDiff(from, to) > 1) {
            int dir = Integer.compare(to, from);
            int rookPos = (dir < 0) ? from - Helper.distanceLeftB(from) : from + Helper.distanceRightB(from);
            rawMove(to - dir, rookPos, false);
        }
    }

    public GameState checkChess(byte[] pseudoLegalMoves) throws CloneNotSupportedException {
        boolean illegalMove = false;
        Piece[] baseState = board.clone();
        int kB = kingBPos;
        int kW = kingWPos;
        boolean hasMoves = false;

        for (int i = 0; i < Board.size; i++) {
            int base = i * MOVE_STRIDE;
            int len = pseudoLegalMoves[base] & 0xFF;
            int writePtr = 0;

            for (int j = 0; j < len; j++) {
                byte move = pseudoLegalMoves[base + 1 + j];
                //System.out.printf("move: %d -> %d\n", i, move);

                rawMove(i, move, false);

                if (turn) {
                    illegalMove = isChecked(kingWPos);
                } else {
                    illegalMove = isChecked(kingBPos);
                }

                kingBPos = kB;
                kingWPos = kW;
                board = baseState.clone();

                if (illegalMove) {
                    continue;
                }
                if (board[i].getColor() == turn) {
                    hasMoves = true;
                }

                // compact: keep only legal moves
                pseudoLegalMoves[base + 1 + writePtr] = move;
                writePtr++;
            }
            pseudoLegalMoves[base] = (byte) writePtr;
        }

        return checkCheckmate(hasMoves);
    }

    private GameState checkCheckmate(boolean hasMove) throws CloneNotSupportedException {
        if (hasMove) {
            return GameState.Continue;
        }

        boolean inCheck;
        if (turn) {
            inCheck = isChecked(kingWPos);
        } else {
            inCheck = isChecked(kingBPos);
        }

        if (inCheck) {
            return GameState.CheckMate;
        }

        return GameState.StaleMate;
    }

    public boolean isChecked(int kingPos) throws CloneNotSupportedException {
        byte[] moveFields = new byte[Board.size * MOVE_STRIDE];
        getPseudoMovesFromPosition(moveFields);

        for (int i = 0; i < Board.size; i++) {
            int base = i * MOVE_STRIDE;
            int len = moveFields[base] & 0xFF;
            for (int j = 0; j < len; j++) {
                if ((moveFields[base + 1 + j] & 0xFF) == kingPos) {
                    return true;
                }
            }
        }
        return false;
    }

    public DiversePair<byte[], GameState> getMovesFromPosition() throws CloneNotSupportedException {
        getPseudoMovesFromPosition(globalLegalMoves);
        return new DiversePair<>(globalLegalMoves, checkChess(globalLegalMoves));
    }

    /**
     * Fills the flat stride array with pseudo-legal moves.
     * For square i:
     *   base = i * MOVE_STRIDE
     *   moves[base]          = count of moves
     *   moves[base + 1 .. n] = target squares
     */
    private void getPseudoMovesFromPosition(byte[] pseudoLegals) throws CloneNotSupportedException {
        moveList.clear();
        byte[] moves = moveList.getMovesArray();

        int pieceCount = 0;

        for (int i = 0; i < board.length; i++) {
            int base = i * MOVE_STRIDE;

            if (board[i].getType() == PieceTypes.EMPTY) {
                pseudoLegals[base] = 0;
                continue;
            }

            board[i].getMoves(i, moveList);

            int dirStart = moveList.getDirectionOffset(pieceCount);
            int dirCount = moveList.getDirectionCount(pieceCount);

            int ptr = 0;

            directionLoop:
            for (int d = 0; d < dirCount; d++) {
                int dirIndex = dirStart + d;
                int moveOffset = moveList.getDirectionMovesOffset(dirIndex);
                int moveLength = moveList.getDirectionMovesLength(dirIndex);

                for (int j = 0; j < moveLength; j++) {
                    byte square = moves[moveOffset + j];

                    if (board[i].getType() == PieceTypes.PAWN) {
                        if (Helper.isDiagonalMove(i, square)) {
                            if (board[square].getColor() != board[i].getColor()
                                    && board[square] != EmptyPiece.EMPTY_PIECE) {
                                pseudoLegals[base + 1 + ptr] = square;
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
                        Piece corner = (dir < 0)
                                ? board[i - Helper.distanceLeftB(i)]
                                : board[i + Helper.distanceRightB(i)];

                        if (Helper.colDiff(i, square) > 1) {
                            if (board[i].isHasMoved()
                                    || corner.isHasMoved()
                                    || corner.getType() != PieceTypes.ROOK) {
                                break;
                            }
                            for (int k = i + dir; k != square; k += dir) {
                                if (board[k] != EmptyPiece.EMPTY_PIECE) {
                                    break directionLoop;
                                }
                            }
                            pseudoLegals[base + 1 + ptr] = square;
                            ptr++;
                            break;
                        }
                    }

                    // blocked → stop this direction
                    if (board[square] != EmptyPiece.EMPTY_PIECE) {
                        if (board[square].getColor() != board[i].getColor()) {
                            pseudoLegals[base + 1 + ptr] = square;
                            ptr++;
                        }
                        break;
                    }

                    pseudoLegals[base + 1 + ptr] = square;
                    ptr++;
                }
            }

            pseudoLegals[base] = (byte) ptr;
            pieceCount++;
        }
    }

    private MoveTypes castleFreeMove(int from, int to, boolean moved) throws CloneNotSupportedException {
        if (board[from] == EmptyPiece.EMPTY_PIECE) {
            return MoveTypes.KingMove;
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
        byte[] allMoves = null;
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
    }
}