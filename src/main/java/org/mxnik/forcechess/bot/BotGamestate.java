package org.mxnik.forcechess.bot;

import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.Util.Bitboard;
import org.mxnik.forcechess.Util.FastBitmap;

/**
 * Hardcoded BotGamestate for 64 x 64 further training may be applied
 */
public class BotGamestate {
    private int boardSize;
    private Bitboard WPawns = new Bitboard();
    private Bitboard BPanws = new Bitboard();
    private Bitboard WKnights = new Bitboard();
    private Bitboard BKnights = new Bitboard();
    private Bitboard WRooks = new Bitboard();
    private Bitboard BRooks = new Bitboard();
    private Bitboard WBishops = new Bitboard();
    private Bitboard BBishops = new Bitboard();
    private Bitboard WQueens = new Bitboard();
    private Bitboard BQueens = new Bitboard();
    private Bitboard WKings = new Bitboard();
    private Bitboard BKings = new Bitboard();
    private Bitboard WKingCastle = new Bitboard();
    private Bitboard BKingCastle = new Bitboard();
    private Bitboard WQueenCastle = new Bitboard();
    private Bitboard BQueenCastle = new Bitboard();


    private void buildFromBoard(Board buildBoard){
        Piece[] b = buildBoard.getBoard();
        for(int i = 0; i < Board.size; i++){
            Piece p = b[i];
            switch (p.getType()){
                case PAWN -> {
                    if (p.getColor()){
                        WPawns.set(i);
                        continue;
                    }
                    BPanws.set(i);
                }
                case BISHOP -> {
                    if (p.getColor()){
                        WBishops.set(i);
                        continue;
                    }
                    BBishops.set(i);
                }
                case KNIGHT -> {
                    if (p.getColor()){
                        WKnights.set(i);
                        continue;
                    }
                    BKnights.set(i);
                }
                case QUEEN -> {
                    if (p.getColor()){
                        WQueens.set(i);
                        continue;
                    }
                    BQueens.set(i);
                }
                case ROOK -> {
                    if (p.getColor()){
                        WRooks.set(i);
                        continue;
                    }
                    BRooks.set(i);
                }
                case KING -> {
                    if (p.getColor()){
                        WKings.set(i);
                        continue;
                    }
                    BKings.set(i);
                }
            }
        }
    }

    public BotGamestate (Board buildBoard){
        buildFromBoard(buildBoard);
    }

    public BotGamestate (String fenStr){
        Board bb = new Board(fenStr);
        buildFromBoard(bb);
    }

    public static void main(String[] args) {
        BotGamestate bg = new BotGamestate("rnbqkbnr/pppppppp/P7/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8");
        System.out.println(bg.WPawns.board);
    }
}
