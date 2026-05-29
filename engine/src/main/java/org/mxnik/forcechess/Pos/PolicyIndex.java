package org.mxnik.forcechess.Pos;


// KI-GENERIERTE KLASSE
// TURNS MOVE INTO ARRAY INDEX TO REDUCE SIZE OF POLICY_VECTOR
// MY GPU KEPT RUNNING OUT OF MEMORY BEFORE::

public class PolicyIndex {

    public static final int POLICY_SIZE = 4672; // 64 * 73

    /**
     * turn a move into a policyIndex <p>
     * this is done to drastically remove the size of the policyV from 2^16 to 4672
     */
    public static int toPolicyIndex(int move) {
        int from  = Move.from(move);
        int to    = Move.to(move);
        int flag  = Move.baseFlag(Move.flags(move)); // strip capture bit

        int fromRank = from / 8, fromFile = from % 8;
        int toRank   = to   / 8, toFile   = to   % 8;
        int dr = toRank   - fromRank;
        int df = toFile   - fromFile;

        int plane = switch (flag) {
            case Move.FLAG_PROMOTE_R -> 64 + (df + 1); // rook,   dirs 0-2
            case Move.FLAG_PROMOTE_B -> 64 + 3 + (df + 1); // bishop, dirs 0-2
            case Move.FLAG_PROMOTE_N -> 64 + 2 * 3 + (df + 1); // knight, dirs 0-2
            // queen promotion falls through to queen plane (no dedicated plane needed)
            default -> isKnightMove(dr, df) ? knightPlane(dr, df) : queenPlane(dr, df);
        };

        return from * 73 + plane;
    }

    private static boolean isKnightMove(int dr, int df) {
        int a = Math.abs(dr), b = Math.abs(df);
        return (a == 2 && b == 1) || (a == 1 && b == 2);
    }

    private static int queenPlane(int dr, int df) {
        // 8 directions x 7 distances = planes 0-55
        int dir = switch (Integer.signum(dr) * 3 + Integer.signum(df)) {
            case  3 -> 0; // N
            case  4 -> 1; // NE
            case  1 -> 2; // E
            case -2 -> 3; // SE
            case -3 -> 4; // S
            case -4 -> 5; // SW
            case -1 -> 6; // W
            case  2 -> 7; // NW
            default -> throw new IllegalArgumentException("Not a sliding move: dr=" + dr + " df=" + df);
        };
        int dist = Math.max(Math.abs(dr), Math.abs(df)) - 1; // 0-6
        return dir * 7 + dist;
    }

    private static int knightPlane(int dr, int df) {
        // encode as dr*10+df, all 8 knight deltas are unique
        return 56 + switch (dr * 10 + df) {
            case  21 -> 0; //  2, 1
            case  19 -> 1; //  2,-1
            case  12 -> 2; //  1, 2
            case   8 -> 3; //  1,-2
            case - 8 -> 4; // -1, 2
            case -12 -> 5; // -1,-2
            case -19 -> 6; // -2, 1
            case -21 -> 7; // -2,-1
            default  -> throw new IllegalArgumentException("Unknown knight: dr=" + dr + " df=" + df);
        };
    }
}