import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;

public class NotationTests {

    @Test
    public void testFenConversion(){
        //full String rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR  w KQkq - 0 1
        FenNotation.readFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", 8);
    }
}
