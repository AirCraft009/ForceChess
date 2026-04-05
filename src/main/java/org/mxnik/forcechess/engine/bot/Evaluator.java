package org.mxnik.forcechess.engine.bot;

import org.mxnik.forcechess.engine.Pos.PositionEncoder;

import java.util.Arrays;

public interface Evaluator{
    Result evaluate(PositionEncoder.Position pos);

    record Result(float[] policyV, float value) {}

    final class StubEvaluator implements Evaluator {
        @Override
        public Result evaluate(PositionEncoder.Position pos) {
            float[] policy = new float[4672];
            Arrays.fill(policy, 1f / 4672);   // uniform prior
            return new Result(policy, 0f);    // draw estimate
        }
    }
}