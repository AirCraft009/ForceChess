package org.mxnik.forcechess.bot;

import org.mxnik.forcechess.Pos.Move;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.util.Arrays;

public interface BatchEvaluator extends Evaluator {

    public Result[] evaluateBatch(float[] inputs);

    final class StubEvaluator implements BatchEvaluator {
        public float[] policy = new float[Move.MOVE_POSSIBILITIES];

        public StubEvaluator(){
            Arrays.fill(policy, 1f / 4672);   // uniform prior
        }

        @Override
        public Result evaluate(PositionEncoder.Position pos) {
            return new Result(policy, pos.whiteMaterial - pos.blackMaterial);    // draw estimate
        }

        public Result evaluate() {
            return new Result(policy, 0);    // draw estimate
        }

        @Override
        public Result[] evaluateBatch(float[] inputs) {
            Result[] r = new Result[inputs.length];
            for (int i = 0; i < inputs.length; i++) {
                r[i] = evaluate();
            }
            return r;
        }
    }
}
