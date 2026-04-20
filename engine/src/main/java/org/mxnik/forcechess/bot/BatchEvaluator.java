package org.mxnik.forcechess.bot;

public interface BatchEvaluator extends Evaluator {

    public Result[] evaluateBatch(float[] inputs);
}
