package org.mxnik.forcechess.MCTS;


import java.util.Random;

/**
 * Ki-generierte Klasse:
 * soll vermeiden, dass Anfangs immer dieselben Moves gewählt werden
 */
public final class RandNoise {
    public final static Random random = new Random();

     static float[] dirichlet(float alpha, int count) {
        float[] samples = new float[count];
        float sum = 0;
        for (int i = 0; i < count; i++) {
            // Gamma(alpha, 1) sample via Marsaglia method for alpha < 1
            samples[i] = (float) gammaRandom(alpha);
            sum += samples[i];
        }
        for (int i = 0; i < count; i++) samples[i] /= sum;
        return samples;
    }

    private static double gammaRandom(double alpha) {
        if (alpha < 1.0) {
            // boost alpha by 1, then scale down using the relation:
            // Gamma(alpha) = Gamma(alpha+1) * U^(1/alpha)
            return gammaRandom(alpha + 1.0) * Math.pow(random.nextDouble(), 1.0 / alpha);
        }
        double d = alpha - 1.0 / 3.0;
        double c = 1.0 / Math.sqrt(9.0 * d);
        while (true) {
            double x = random.nextGaussian();
            double v = 1.0 + c * x;
            if (v <= 0) continue;
            v = v * v * v;
            double u = random.nextDouble();
            if (u < 1.0 - 0.0331 * (x * x) * (x * x)) return d * v;
            if (Math.log(u) < 0.5 * x * x + d * (1.0 - v + Math.log(v))) return d * v;
        }
    }

}
