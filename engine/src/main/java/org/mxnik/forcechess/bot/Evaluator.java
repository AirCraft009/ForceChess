package org.mxnik.forcechess.bot;

import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * evaluator interface to hide complexity and make testing easy
 */
public interface Evaluator extends Closeable {
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

        @Override
        public void close() throws IOException {

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

        @Override
        public void close() throws IOException {

        }
    }
}