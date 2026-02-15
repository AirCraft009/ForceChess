import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

import static org.junit.jupiter.api.Assertions.*;

public class NotationTests {

    @Test
    public void testReadFenParsesStartingPosition() {
        Piece[] board = FenNotation.readFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", 8);

        assertNotNull(board);
        assertEquals(64, board.length);

        // White back rank (index 0..7)
        assertEquals(PieceTypes.ROOK, board[0].getType());
        assertEquals(0, board[0].getColor());
        assertEquals(PieceTypes.KING, board[4].getType());
        assertEquals(0, board[4].getColor());

        // Black back rank (index 56..63)
        assertEquals(PieceTypes.ROOK, board[56].getType());
        assertEquals(1, board[56].getColor());
        assertEquals(PieceTypes.KING, board[60].getType());
        assertEquals(1, board[60].getColor());

        // Middle board squares should be empty
        assertSame(Piece.emptyPiece, board[27]);
        assertSame(Piece.emptyPiece, board[36]);
    }

    @Test
    public void testReadFenRejectsIllegalCharacter() {
        FenException ex = assertThrows(FenException.class, () -> FenNotation.readFen("7x/8/8/8/8/8/8/8", 8));
        assertTrue(ex.getMessage().contains("Illegal Char"));
    }

    @Test
    public void testReadFenRejectsIncompleteFen() {
        FenException ex = assertThrows(FenException.class, () -> FenNotation.readFen("8/8/8/8/8/8/8", 8));
        assertTrue(ex.getMessage().contains("Fen isn't complete"));
    }

    @Test
    public void testReadFenRejectsInvalidRowWidth() {
        FenException ex = assertThrows(FenException.class, () -> FenNotation.readFen("7/8/8/8/8/8/8/8", 8));
        assertTrue(ex.getMessage().contains("don't match the sidelen"));
    }

    @Test
    public void testWriteFenCurrentBehaviorReturnsNullLiteral() {
        Piece[] board = new Piece[64];
        for (int i = 0; i < board.length; i++) {
            board[i] = Piece.emptyPiece;
        }

        assertEquals("null", FenNotation.writeFen(board));
    }
}
