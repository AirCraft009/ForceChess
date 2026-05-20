package user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Board.Board;
import org.mxnik.forcechess.ChessLogic.Board.ChessMoveGen;
import org.mxnik.forcechess.Moves.GameState;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BoardTests {
    Board b;

    @BeforeEach
    public void setupStandardB(){
        b = new Board();        // initialize with standard string
    }

    @Nested
    @DisplayName("GameStateTests")
    class GameStateTests{

        @Test
        @DisplayName("FiftyMoveRule Stalemate")
        public void fiftyMoveStalemate() throws CloneNotSupportedException {
            b.setFiftyMove(100);        // fiftyMoveCounter at 100 should lead to stalemate
            assertEquals(GameState.FiftyMove, ChessMoveGen.getMovesFromPosition(b).second());
        }
    }
}
