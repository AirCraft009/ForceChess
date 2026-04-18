package user;

import org.junit.jupiter.api.Test;
import org.mxnik.forcechess.ChessLogic.Board;
import org.mxnik.forcechess.ChessLogic.Notation.FenException;
import org.mxnik.forcechess.ChessLogic.Notation.FenReader;
import org.mxnik.forcechess.ChessLogic.Notation.FenWriter;
import org.mxnik.forcechess.ChessLogic.Pieces.EmptyPiece;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;
import org.mxnik.forcechess.ChessLogic.Pieces.PieceTypes;

import static org.junit.jupiter.api.Assertions.*;

public class NotationTests {

    @Test
    public void testReadFenParsesStartingPosition() {
        FenReader fenNotation = new FenReader("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w q - 1 8");
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
        assertFalse(board[56].getColor());
        assertEquals(PieceTypes.KING, board[60].getType());
        assertFalse(board[60].getColor());

        // Middle board squares should be empty
        assertSame(EmptyPiece.EMPTY_PIECE, board[27]);
        assertSame(EmptyPiece.EMPTY_PIECE, board[36]);
    }

    @Test
    public void testReadFenRejectsIllegalCharacter() {
        FenReader f = new FenReader("7x/8/8/8/8/8/8/8 w q - 1 8");
        FenException ex = assertThrows(FenException.class, f::readFenBoard);
        assertTrue(ex.getMessage().contains("Illegal char"));
    }

    @Test
    public void testReadFenRejectsIncompleteFen() {
        assertThrows(FenException.class, () -> new FenReader("8/8/8/8/8/8/8"));
    }

    @Test
    public void testReadFenRejectsInvalidRowWidth() {
        FenReader f= new FenReader("7/8/8/8/8/8/8/8 w q - 1 8");
        FenException ex = assertThrows(FenException.class, f::readFenBoard);
    }


    @Test
    public void testWriteFenStartingPos(){
        Board b = new Board();
        String fenStr = FenWriter.WriteFen(b);
        assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w 0 0 0 8", fenStr);
    }
}
