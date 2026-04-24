package org.mxnik.forcechess.global;

public enum MoveType {
    Generic(0),
    CastleK(1),
    CastleQ(2),
    EnPassant(3),
    PromotionQ(4),
    PromotionR(5),
    PromotionB(6),
    PromotionN(7);

    public final int flagVal;

    MoveType(int flagVal) {
        this.flagVal = flagVal;
    }

    public static MoveType fromFlagVal(int flagVal){
        return MoveType.values()[flagVal];
    }
}
