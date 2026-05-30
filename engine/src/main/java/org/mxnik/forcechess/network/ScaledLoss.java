package org.mxnik.forcechess.network;
import org.nd4j.common.primitives.Pair;
import org.nd4j.linalg.activations.IActivation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.lossfunctions.impl.*;

/**
 * UNUSED CLASS WILL GET REMOVED AFTER FINALIZATION
 */
public class ScaledLoss implements ILossFunction {

    private final ILossFunction baseLoss;
    private final double scale;

    public ScaledLoss(ILossFunction baseLoss, double scale) {
        this.baseLoss = baseLoss;
        this.scale = scale;
    }

    @Override
    public double computeScore(
            INDArray labels,
            INDArray preOutput,
            IActivation activationFn,
            INDArray mask,
            boolean average) {

        return scale * baseLoss.computeScore(
                labels, preOutput, activationFn, mask, average);
    }

    @Override
    public INDArray computeScoreArray(
            INDArray labels,
            INDArray preOutput,
            IActivation activationFn,
            INDArray mask) {

        return baseLoss.computeScoreArray(
                        labels,
                        preOutput,
                        activationFn,
                        mask)
                .muli(scale);
    }

    @Override
    public INDArray computeGradient(
            INDArray labels,
            INDArray preOutput,
            IActivation activationFn,
            INDArray mask) {

        return baseLoss.computeGradient(
                        labels, preOutput, activationFn, mask)
                .muli(scale);
    }

    @Override
    public Pair<Double, INDArray> computeGradientAndScore(INDArray labels, INDArray preOutput, IActivation activationFn, INDArray mask, boolean average) {
        return new Pair<>(computeScore(labels, preOutput, activationFn, mask, average), computeGradient(labels, preOutput, activationFn, mask));
    }

    @Override
    public String name() {
        return baseLoss.name() + "_Scaled";
    }


}