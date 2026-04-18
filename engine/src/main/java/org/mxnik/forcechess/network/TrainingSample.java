package org.mxnik.forcechess.network;


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
