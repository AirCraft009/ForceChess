package org.mxnik.forcechess.bot;

import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.util.Arrays;

public interface Evaluator{
    Result evaluate(PositionEncoder.Position pos);

    record Result(float[] policyV, float value) {}

    final class StubEvaluator implements Evaluator {
        public float[] policy = new float[Move.MOVE_POSSIBILITIES];

        public StubEvaluator(){
            Arrays.fill(policy, 1f / 4672);   // uniform prior
        }

        @Override
        public Result evaluate(PositionEncoder.Position pos) {
            return new Result(policy, pos.whiteMaterial - pos.blackMaterial);    // draw estimate
        }
    }
}