package org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Moves.MoveList;
import org.mxnik.forcechess.ChessLogic.Moves.MoveOffsets;
import org.mxnik.forcechess.ChessLogic.Moves.UndoMovePacket;
import org.mxnik.forcechess.ChessLogic.Pieces.*;
import org.mxnik.forcechess.General.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenReader;
import org.mxnik.forcechess.ChessLogic.Notation.FenWriter;

import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Moves.MovePacket;
import org.mxnik.forcechess.Moves.MoveType;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;

import static org.mxnik.forcechess.ChessLogic.Notation.FenConversion.FromPiece;
import static org.mxnik.forcechess.Moves.RayDetection.*;


/**
 * internal representation of a Chessboard.
 */
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
    int kingWPos;
    int kingBPos;
    int enPassantPos;
    int fiftyMove;

    int maxMoves = 0;

    /**
     * initializes a Board with the normal chess starting pos.
     */
    public Board() {
        board = new Piece[sideLen * sideLen];
        BuildFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
    }

    /**
     * initializes a Board with a given position
     */
    public Board(String fenString) {
        BuildFromFen(fenString);
        MoveOffsets.calculateOffset(sideLen);
    }


    public void calcMaterial(){
        teamBMaterial = 0;
        teamWMaterial = 0;

        for (Piece p : board){
            if (p == EmptyPiece.EMPTY_PIECE)
                continue;

            if(p.getColor()){
                teamWMaterial += p.getType().value;
            }else {
                teamBMaterial += p.getType().value;
            }
        }
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
     *
     * Verwendet Ray-Modell beginnt vom könig aus in alle Richtungen (slide) und Springerform
     * und check für Schach.
     *
     * @param kingPos   flat board index of the king to test
     * @param kingColor true = white king, false = black king
     * @return is the king in check
     */
    public boolean isChecked(int kingPos, boolean kingColor) {
        if(kingPos == -1)
            return false;

        int kingRow = BoardHelper.getRow(kingPos);
        int kingCol = BoardHelper.getCol(kingPos);

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

    private boolean castleFreeMove(int from, int to, MoveType type, boolean moved) throws CloneNotSupportedException {
        if (board[from] == EmptyPiece.EMPTY_PIECE) {
            return true;
        }
        if(moved) {
            int pawnDir = (board[from].getColor())? 1:-1;
            if(board[from].getType() == PieceTypes.PAWN && BoardHelper.isDiagonalMove(from, to) && to == enPassantPos){
                //En Passant take
                board[to + MoveOffsets.DOWN.offset*pawnDir] = EmptyPiece.EMPTY_PIECE;
                amountPieces -= 1;
            }
            if (board[from].getType() == PieceTypes.PAWN
                    && BoardHelper.rowDiff(from, to) > 1) {
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


        switch (type){
            case PromotionN -> board[to] = new Knight(p.getColor(), true);
            case PromotionB -> board[to] = new Bishop(p.getColor(), true);
            case PromotionR -> board[to] = new Rook(p.getColor(), true);
            case PromotionQ -> board[to] = new Queen(p.getColor(), true);
        }


        return p.getType() == PieceTypes.KING;
    }

    void rawMove(int from, int to, MoveType type, boolean moved) throws CloneNotSupportedException {
        if (!castleFreeMove(from, to, type, moved)) {
            return;
        }

        if (turn) {
            kingWPos = to;
        } else {
            kingBPos = to;
        }

        if (BoardHelper.colDiff(from, to) > 1) {
            int dir = Integer.compare(to, from);
            int rookPos = (dir < 0)
                    ? from - BoardHelper.distanceLeftB(from)
                    : from + BoardHelper.distanceRightB(from);
            rawMove(rookPos, to - dir, ((dir < 0)? MoveType.CastleQ : MoveType.CastleK), moved);
        }


    }

    public UndoMovePacket move(MovePacket packet) throws CloneNotSupportedException {
        var undoInfo = new UndoMovePacket(packet, board[packet.from()].clone(),
                board[packet.to()] = (board[packet.to()] == EmptyPiece.EMPTY_PIECE? EmptyPiece.EMPTY_PIECE : board[packet.to()].clone()),
                enPassantPos,
                fiftyMove
        );
        rawMove(packet.from(), packet.to(), packet.type(), true);
        turn = !turn;
        fiftyMove++;        // add to fifty move rule counter (counts half moves)

        // check if move is a zeroing move
        if(packet.capture() | board[packet.to()].getType() == PieceTypes.PAWN){
            fiftyMove = 0;
        }

        return undoInfo;
    }

    /**
     * undoes a move (board state is restored perfectly)
     */
    public void undoMove(UndoMovePacket undoPacket){
        System.out.println("undoing updated");
        fiftyMove = undoPacket.fiftyMoveCounter();
        turn = !turn;

        MovePacket packet = undoPacket.packet();

        try {
            switch (packet.type()) {
                case Generic -> {
                    board[packet.from()] = undoPacket.movedP();
                    board[packet.to()] = undoPacket.takenP();
                }
                case PromotionB, PromotionN, PromotionQ, PromotionR -> {
                    board[packet.from()] = new Pawn(turn, true);
                    board[packet.to()] = undoPacket.takenP();
                }
                case EnPassant -> {
                    int dir = Math.clamp(packet.from() - packet.to(), -1, 1) * size;       // get the direction then multiply to the size of the board
                    int takeSq = packet.to() - dir;
                    System.out.println(takeSq);
                    board[packet.from()] = undoPacket.movedP();
                    board[packet.to()] = EmptyPiece.EMPTY_PIECE;
                    board[takeSq] = new Pawn(undoPacket.movedP().getColor(), true);     // has to have moved
                    enPassantPos = undoPacket.enPassant();
                }
                case CastleK -> {
                    board[packet.from()] = undoPacket.movedP();
                    board[packet.to()] = EmptyPiece.EMPTY_PIECE;
                    int dr = BoardHelper.distanceRightB(packet.to()-1);         // rook at king prev to pos -1
                    board[(packet.to() -1) + dr] = board[packet.to() -1].clone();    // move rook to corner
                    board[packet.to() - 1] = EmptyPiece.EMPTY_PIECE;
                }

                case CastleQ -> {
                    board[packet.from()] = undoPacket.movedP();
                    board[packet.to()] = EmptyPiece.EMPTY_PIECE;
                    int dr = BoardHelper.distanceRightB(packet.to()+1);         // rook at king prev to pos +1
                    board[(packet.to() +1) + dr] = board[packet.to() +1].clone();    // move rook to corner
                    board[packet.to()  +1] = EmptyPiece.EMPTY_PIECE;
                }
            }

        }catch (CloneNotSupportedException _){}
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

    public static int getSideLen() {
        return sideLen;
    }

    public static void setSideLen(int sideLen) {
        Board.sideLen = sideLen;
    }

    public static int getSize() {
        return size;
    }

    public static void setSize(int size) {
        Board.size = size;
    }

    public void setTurn(boolean turn) {
        this.turn = turn;
    }

    public int getTotalMaterial() {
        return totalMaterial;
    }

    public void setTotalMaterial(int totalMaterial) {
        this.totalMaterial = totalMaterial;
    }

    public int getMaxDirs() {
        return maxDirs;
    }

    public void setMaxDirs(int maxDirs) {
        this.maxDirs = maxDirs;
    }

    public int getAmountPieces() {
        return amountPieces;
    }

    public void setAmountPieces(int amountPieces) {
        this.amountPieces = amountPieces;
    }

    public MoveList getMoveList() {
        return moveList;
    }

    public void setMoveList(MoveList moveList) {
        this.moveList = moveList;
    }

    public int getTeamWMaterial() {
        return teamWMaterial;
    }

    public void setTeamWMaterial(int teamWMaterial) {
        this.teamWMaterial = teamWMaterial;
    }

    public int getTeamBMaterial() {
        return teamBMaterial;
    }

    public void setTeamBMaterial(int teamBMaterial) {
        this.teamBMaterial = teamBMaterial;
    }

    public int getKingWPos() {
        return kingWPos;
    }

    public void setKingWPos(int kingWPos) {
        this.kingWPos = kingWPos;
    }

    public int getKingBPos() {
        return kingBPos;
    }

    public void setKingBPos(int kingBPos) {
        this.kingBPos = kingBPos;
    }

    public int getEnPassantPos() {
        return enPassantPos;
    }

    public void setEnPassantPos(int enPassantPos) {
        this.enPassantPos = enPassantPos;
    }

    public int getFiftyMove() {
        return fiftyMove;
    }

    public void setFiftyMove(int fiftyMove) {
        this.fiftyMove = fiftyMove;
    }

    public int getMaxMoves() {
        return maxMoves;
    }

    public void setMaxMoves(int maxMoves) {
        this.maxMoves = maxMoves;
    }

    public static void main(String[] args) throws CloneNotSupportedException {
        Board board1 = new Board("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        long starT = System.nanoTime();
        byte[][] allMoves = null;
        for (int i = 0; i < 1000000; i++) {
            allMoves = ChessMoveGen.getMovesFromPosition(board1).first();
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
