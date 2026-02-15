import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Moves.Helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelperTests {

    @BeforeEach
    public void setupBoardSideLen() {
        Board.sideLen = 8;
    }

    @Test
    public void testGetRowAndGetCol() {
        assertEquals(0, Helper.getRow(0));
        assertEquals(0, Helper.getCol(0));

        assertEquals(3, Helper.getRow(27));
        assertEquals(3, Helper.getCol(27));

        assertEquals(7, Helper.getRow(63));
        assertEquals(7, Helper.getCol(63));
    }

    @Test
    public void testDistanceLeftAndRightBorders() {
        assertEquals(0, Helper.distanceLeftB(0));
        assertEquals(7, Helper.distanceRightB(0));

        assertEquals(3, Helper.distanceLeftB(27));
        assertEquals(4, Helper.distanceRightB(27));

        assertEquals(7, Helper.distanceLeftB(63));
        assertEquals(0, Helper.distanceRightB(63));
    }

    @Test
    public void testDistanceTopAndBottomBorders() {
        assertEquals(64, Helper.distanceTopB(0));
        assertEquals(0, Helper.distanceBottomB(0));

        assertEquals(40, Helper.distanceTopB(27));
        assertEquals(24, Helper.distanceBottomB(27));

        assertEquals(8, Helper.distanceTopB(63));
        assertEquals(56, Helper.distanceBottomB(63));
    }
}
