package org.mxnik.forcechess.stockfish;
import org.stockfish.StockfishJNI;
import org.stockfish.StockfishJNI.*;

public class Stockfish_testing {
    StockfishJNI stockfishJNI = new StockfishJNI();

    public void test(){
        stockfishJNI.setPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR  w KQkq - 0 1");
        System.out.println("set position");
        //stockfishJNI.setPosition("startpos", new String[]{});
        System.out.println(stockfishJNI.staticEvalCp());
    }

    public static void main(String[] args) {
        Stockfish_testing testing = new Stockfish_testing();
        testing.test();
    }

}