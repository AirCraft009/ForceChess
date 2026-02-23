import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenNotation;
import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

import static org.junit.jupiter.api.Assertions.*;

public class NotationTests {

    @Test
    public void testReadFenParsesStartingPosition() {
        FenNotation fenNotation = new FenNotation("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w q - 1 8");
        Piece[] board = fenNotation.readFenBoard();


        assertEquals(8, fenNotation.getBoardLenght());
        assertNotNull(board);
        assertEquals(64, board.length);

        // White back rank (index 0..7)
        assertEquals(PieceTypes.ROOK, board[0].getType());
        assertTrue(board[0].getColor());
        assertEquals(PieceTypes.KING, board[4].getType());
        assertTrue(board[4].getColor());

        // Black back rank (index 56..63)
        assertEquals(PieceTypes.ROOK, board[56].getType());
        assertTrue(board[56].getColor());
        assertEquals(PieceTypes.KING, board[60].getType());
        assertTrue(board[60].getColor());

        // Middle board squares should be empty
        assertSame(EmptyPiece.EMPTY_PIECE, board[27]);
        assertSame(EmptyPiece.EMPTY_PIECE, board[36]);
    }

    @Test
    public void testReadFenRejectsIllegalCharacter() {
        FenException ex = assertThrows(FenException.class, () -> new FenNotation("7x/8/8/8/8/8/8/8 w q - 1 8"));
        assertTrue(ex.getMessage().contains("Illegal Char"));
    }

    @Test
    public void testReadFenRejectsIncompleteFen() {
        FenException ex = assertThrows(FenException.class, () -> new FenNotation("8/8/8/8/8/8/8"));
        assertTrue(ex.getMessage().contains("Fen isn't complete"));
    }

    @Test
    public void testReadFenRejectsInvalidRowWidth() {
        FenException ex = assertThrows(FenException.class, () -> new FenNotation("7/8/8/8/8/8/8/8"));
        assertTrue(ex.getMessage().contains("don't match the sidelen"));
    }

    @Test
    public void testWriteFenCurrentBehaviorReturnsNullLiteral() {
        Piece[] board = new Piece[64];
        for (int i = 0; i < board.length; i++) {
            board[i] = EmptyPiece.EMPTY_PIECE;
        }

        assertEquals("null", FenNotation.writeFen(board));
    }
}
