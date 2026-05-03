package engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.Pos.PositionEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mxnik.forcechess.Pos.PositionUtils.fromFen;
import static org.mxnik.forcechess.Pos.PositionUtils.toFen;

@DisplayName("Fen validity")
public class FenValidityTests {

    @Test
    @DisplayName("Does StartingPosition equal when generated via fen or hardcoded")
    void starterFenToPos(){
        var fenPos = fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        var startPos = PositionEncoder.Position.StartingPosition();

        assertEquals(startPos, fenPos);
    }

    @Test
    @DisplayName("Is Startpos fen correct")
    void starterPosToFen(){
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", toFen(PositionEncoder.Position.StartingPosition()));
    }


    @Test
    @DisplayName("From Fen to Pos to Fen")
    void interconnected() {
        var fenPos = fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        var startPos = PositionEncoder.Position.StartingPosition();

        assertEquals(startPos, fenPos);

        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", toFen(fenPos));
    }
}
