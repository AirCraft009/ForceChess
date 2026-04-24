package org.mxnik.forcechess.network_bot_interactions.Pos;


// Info to unmake a move packed into an int
// bits 0  - 15 : Move (see bit desc. in Move class)
// bits 16 - 19 : TakenPiece (Type and Color of the taken piece -> Empty Piece if none were taken
// bits 20 - 23 : Prev. Castle-perms (defined in the Position class)
public final class UndoMoveInfo {

    public static int of(int move, int takenPiece, byte castlePerms){return move | takenPiece << TAKEN_PIECE_SHIFT | castlePerms << CASTLE_PERM_SHIFT;}
    public static int move(int info){return info & MOVE_MASK;}
    public static int from(int info){return Move.from(move(info));}
    public static int to(int info){return Move.to(move(info));}
    public static byte castlePerms(int info){return (byte)((info >>> CASTLE_PERM_SHIFT) & CASTLE_PERM_MASK);}
    public static int takenPiece(int info){ return (info >>> TAKEN_PIECE_SHIFT) & TAKEN_PIECE_MASK;}

    private final static int MOVE_MASK = 0x7FFF;
    private final static int TAKEN_PIECE_MASK = 0xF;
    private final static int CASTLE_PERM_MASK = 0xF;

    private final static int TAKEN_PIECE_SHIFT = 16;
    private final static int CASTLE_PERM_SHIFT = 20;


    public static void main(String[] args) {
        int move = 0b111111111111111;
        int takenP = Piece.of(Piece.BLACK, Piece.PAWN);
        byte perms = 0b1111;

        int Undo =  UndoMoveInfo.of(move, takenP, perms);
        System.out.println(Integer.toBinaryString(UndoMoveInfo.move(Undo)));
        System.out.println(Integer.toBinaryString(UndoMoveInfo.takenPiece(Undo)));
        System.out.println(Integer.toBinaryString(UndoMoveInfo.castlePerms(Undo)));
    }

}
