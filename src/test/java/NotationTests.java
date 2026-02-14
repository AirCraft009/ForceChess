import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;

public class NotationTests {

    @Test
    public void testFenConversion(){
        FenNotation.readFen("k2s/3", 10);
    }
}
