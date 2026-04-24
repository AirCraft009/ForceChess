package org.mxnik.forcechess.network_bot_interactions.training;


public final class TrainingSample {
    public final float[] tensor;
    public final float[] pi;
    public  float z;

    public TrainingSample(float[] tensor, float[] pi, float z){
        this.tensor = tensor;
        this.pi = pi;
        this.z = z;
    }

}
