package org.mxnik.forcechess.engine.bot;

import org.mxnik.forcechess.Util.DiversePair;
import org.mxnik.forcechess.engine.Pos.PositionEncoder;

import java.util.Random;

/**
 * connects
 */
public class AlphaNet {
    static Random r = new Random();

    /**
     * takes a state and action set and runs it through the network
     * currently a stub
     * @param state the current chessboard position
     * @param action a move possible in the state
     * @return A rating of a pos. and a policy vector
     */
    public static DiversePair<Float, float[]> runNet(PositionEncoder.Position state, Integer action){
        return new DiversePair<>(r.nextFloat(0, 10), new float[256]);
    }
}
