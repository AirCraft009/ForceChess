package user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.BoardHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BoardHelperTests {

    @BeforeEach
    public void setupBoardSideLen() {
        Board.sideLen = 8;
    }

    @Test
    public void testGetRowAndGetCol() {
        assertEquals(0, BoardHelper.getRow(0));
        assertEquals(0, BoardHelper.getCol(0));

        assertEquals(3, BoardHelper.getRow(27));
        assertEquals(3, BoardHelper.getCol(27));

        assertEquals(7, BoardHelper.getRow(63));
        assertEquals(7, BoardHelper.getCol(63));
    }

    @Test
    public void testDistanceLeftAndRightBorders() {
        assertEquals(0, BoardHelper.distanceLeftB(0));
        assertEquals(7, BoardHelper.distanceRightB(0));

        assertEquals(3, BoardHelper.distanceLeftB(27));
        assertEquals(4, BoardHelper.distanceRightB(27));

        assertEquals(7, BoardHelper.distanceLeftB(63));
        assertEquals(0, BoardHelper.distanceRightB(63));
    }

    @Test
    public void testDistanceTopAndBottomBorders() {
        assertEquals(7, BoardHelper.distanceTopB(0));
        assertEquals(0, BoardHelper.distanceBottomB(0));

        assertEquals(4, BoardHelper.distanceTopB(27));
        assertEquals(3, BoardHelper.distanceBottomB(27));

        assertEquals(0, BoardHelper.distanceTopB(63));
        assertEquals(7, BoardHelper.distanceBottomB(63));
    }
}
