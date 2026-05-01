package org.mxnik.forcechess.ChessLogic.Board;

import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;
import org.mxnik.forcechess.DiversePair;
import org.mxnik.forcechess.GameState;

import java.util.Arrays;

public class ChessMoveGen {
    public static DiversePair<byte[][], GameState> getMovesFromPosition(Board board) throws CloneNotSupportedException {
        byte[][] legalMoves = getPseudoMovesFromPosition(board);
        return new DiversePair<>(legalMoves, checkChess(board, legalMoves));
    }

    /**
     * Generates pseudo-legal moves for all squares.
     * Does not filter for leaving own king in check — that is done in checkChess.
     */
    private static byte[][] getPseudoMovesFromPosition(Board cBoard) {
        cBoard.moveList.clear();
        byte[] moves = cBoard.moveList.getMovesArray();
        byte[][] legalMoves = new byte[Board.size][];

        int pieceCount = 0;
        int prevMoveCount = cBoard.moveList.getMoveCount();

        for (int i = 0; i < cBoard.board.length; i++) {
            if (cBoard.board[i].getType() == PieceTypes.EMPTY) {
                legalMoves[i] = new byte[0];
                continue;
            }

            cBoard.board[i].getMoves(i, cBoard.moveList);

            int newMoveCount = cBoard.moveList.getMoveCount();
            int dirStart = cBoard.moveList.getDirectionOffset(pieceCount);
            int dirCount = cBoard.moveList.getDirectionCount(pieceCount);

            int ptr = 0;
            byte[] legalMoveSection = new byte[newMoveCount - prevMoveCount];
            prevMoveCount = newMoveCount;

            for (int d = 0; d < dirCount; d++) {
                int dirIndex = dirStart + d;
                int moveOffset = cBoard.moveList.getDirectionMovesOffset(dirIndex);
                int moveLength = cBoard.moveList.getDirectionMovesLength(dirIndex);

                int j;
                moveLoop:
                for (j = 0; j < moveLength; j++) {
                    byte square = moves[moveOffset + j];

                    if (cBoard.board[i].getType() == PieceTypes.PAWN) {
                        if (BoardHelper.isDiagonalMove(i, square)) {
                            if ((cBoard.board[square].getColor() != cBoard.board[i].getColor()
                                    && cBoard.board[square] != EmptyPiece.EMPTY_PIECE)
                                    || square == cBoard.enPassantPos) {
                                legalMoveSection[ptr] = square;
                                ptr++;
                            }
                            break;
                        } else {
                            if (cBoard.board[square] != EmptyPiece.EMPTY_PIECE) {
                                break;
                            }
                        }
                    } else if (cBoard.board[i].getType() == PieceTypes.KING) {
                        int dir = Integer.compare(square, i);
                        int cornerPos = (dir < 0)
                                ? i - BoardHelper.distanceLeftB(i)
                                : i + BoardHelper.distanceRightB(i);
                        Piece corner = cBoard.board[cornerPos];

                        if (BoardHelper.colDiff(i, square) > 1) {
                            if (cBoard.board[i].isHasMoved()
                                    || corner.isHasMoved()
                                    || corner.getType() != PieceTypes.ROOK) {
                                break;
                            }
                            for (int k = i + dir; k != cornerPos; k += dir) {
                                if (cBoard.board[k] != EmptyPiece.EMPTY_PIECE || cBoard.isChecked(k,cBoard.board[i].getColor())) {
                                    break moveLoop;
                                }
                            }
                            legalMoveSection[ptr] = square;
                            ptr++;
                            break;
                        }
                    }

                    if (cBoard.board[square] != EmptyPiece.EMPTY_PIECE) {
                        if (cBoard.board[square].getColor() != cBoard.board[i].getColor()) {
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

    /**
     * Filters pseudo-legal moves down to legal moves by making each move,
     * running incremental check detection, then restoring the cBoard.board.
     * Modifies pseudoLegalMoves in place and returns the resulting game state.
     */
    public static GameState checkChess(Board cBoard, byte[][] pseudoLegalMoves) throws CloneNotSupportedException {
        Piece[] baseState = cBoard.board.clone();
        int kB = cBoard.kingBPos;
        int kW = cBoard.kingWPos;
        boolean hasMoves = false;

        for (int i = 0; i < pseudoLegalMoves.length; i++) {
            byte[] moves = pseudoLegalMoves[i];
            int writePtr = 0;

            for (int j = 0; j < moves.length; j++) {
                byte move = moves[j];

                cBoard.rawMove(i, move, false);

                // isChecked now uses incremental ray casting instead of full
                // move generation — the hot path is now ~80 ops instead of ~300+
                boolean inCheck = cBoard.getTurn()
                        ? cBoard.isChecked(cBoard.kingWPos, true)
                        : cBoard.isChecked(cBoard.kingBPos, false);

                cBoard.kingBPos = kB;
                cBoard.kingWPos = kW;
                cBoard.board = baseState.clone();

                if (inCheck) continue;

                if (cBoard.board[i].getColor() == cBoard.getTurn()) {
                    hasMoves = true;
                }

                moves[writePtr] = move;
                writePtr++;
            }

            pseudoLegalMoves[i] = Arrays.copyOf(moves, writePtr);
        }

        //System.out.println(cBoard.toStringBoard());

        return checkCheckmate(cBoard, hasMoves);
    }

    private static GameState checkCheckmate(Board cBoard, boolean hasMove) throws CloneNotSupportedException {
        if (hasMove) {
            return GameState.Continue;
        }

        // No moves: distinguish checkmate from stalemate
        boolean inCheck = cBoard.getTurn()
                ? cBoard.isChecked(cBoard.kingWPos, true)
                : cBoard.isChecked(cBoard.kingBPos, false);

        return inCheck ? GameState.CheckMate : GameState.StaleMate;
    }
}
