package org.mxnik.forcechess.ChessLogic.Board;

import jdk.jfr.Percentage;
import org.mxnik.forcechess.ChessLogic.Pieces.Piece;

public final class CastleRights implements Cloneable{

        public boolean WK_Castle;
        public boolean WQ_Castle;
        public boolean BK_Castle;
        public boolean BQ_Castle;
        public boolean ANY;

        public CastleRights(
                boolean WK_Castle,
                boolean WQ_Castle,
                boolean BK_Castle,
                boolean BQ_Castle
                ){

                this.WK_Castle = WK_Castle;
                this.WQ_Castle = WQ_Castle;
                this.BK_Castle = BK_Castle;
                this.BQ_Castle = BQ_Castle;
                this.ANY = WK_Castle||WQ_Castle||BK_Castle||BQ_Castle;
        }

        public void disableColor(boolean color){
                if(color)
                        disableWhite();
                else
                        disableBlack();
        }

        public void disableWhite(){
                WK_Castle = false;
                WQ_Castle = false;
                this.ANY = BK_Castle || BQ_Castle;
        }

        public void disableBlack(){
                BK_Castle = false;
                BQ_Castle = false;
                this.ANY = WK_Castle || WQ_Castle;
        }

        public void disableKingCastle(boolean color){
                if(color)
                        WK_Castle = false;
                else
                        BK_Castle = false;
                this.ANY = WK_Castle||WQ_Castle||BK_Castle||BQ_Castle;
        }

        public void disableQueenCastle(boolean color){
                if(color)
                        WQ_Castle = false;
                else
                        BQ_Castle = false;
                this.ANY = WK_Castle||WQ_Castle||BK_Castle||BQ_Castle;
        }

        @Override
        public CastleRights clone() throws CloneNotSupportedException {
                return (CastleRights) super.clone();
        }
}
