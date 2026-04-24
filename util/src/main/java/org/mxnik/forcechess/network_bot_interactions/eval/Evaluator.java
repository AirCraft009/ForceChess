package org.mxnik.forcechess.network_bot_interactions.eval;

import org.mxnik.forcechess.network_bot_interactions.Pos.Move;
import org.mxnik.forcechess.network_bot_interactions.Pos.PositionEncoder;

import java.util.Arrays;
import java.util.Random;

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

    final class RandomEvaluator implements Evaluator {
        public float[] policy = new float[Move.MOVE_POSSIBILITIES];
        public final Random r = new Random();


        @Override
        public Result evaluate(PositionEncoder.Position pos) {
            for (int i = 0; i < policy.length; i++) {
                policy[i] = r.nextFloat(10);
            }
            return new Result(policy, pos.whiteMaterial - pos.blackMaterial);    // draw estimate
        }
    }
}