package org.mxnik.forcechess.engine.network;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Random;

public class SampleBuffer {
    public final int lenght;
    private int ptr;
    private TrainingSample[] samples;
    private Random random = new Random();

    public SampleBuffer(int lenght){
        int tempLenght = 1;
        if(lenght > 0)
            tempLenght = lenght;

        this.lenght = tempLenght;
        this.ptr = 0;
        samples = new TrainingSample[lenght];
    }

    public void addSample(TrainingSample s){
        samples[ptr] = s;
        ptr++;
    }

    public void addSample(float[] input, float[] pi, float z){
        samples[ptr] = new TrainingSample(input, pi, z);
        ptr++;
    }

    public TrainingSample sample(){
        int ind = random.nextInt(ptr);
        return samples[ind];
    }

}
