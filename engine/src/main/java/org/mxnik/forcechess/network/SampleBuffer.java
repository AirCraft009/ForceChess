package org.mxnik.forcechess.network;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.mxnik.forcechess.FileLocations;
import org.mxnik.forcechess.Pos.Move;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Deque;
import java.util.Random;

import static org.mxnik.forcechess.Pos.Move.MOVE_POSSIBILITIES;
import static org.mxnik.forcechess.Pos.Move.to;
import static org.mxnik.forcechess.Pos.PositionEncoder.TENSOR_SIZE;

public class SampleBuffer {
    public int length;
    private int ptr;
    private TrainingSample[] samples;
    private final Random random = new Random();
    private final String fullPath;
    private final float DECAY = 0.0F;
    public final static String BASE_PATH = FileLocations.SAMPLE_LOCATIONS;
    private final static float lambda = 0.8F;

    /**
     * creates a sample buffer with a given capacity;
     * @param filename name used to manage the writing and reading data to file
     */
    public SampleBuffer(int length, String filename){
        fullPath = BASE_PATH + filename;
        int templength = 1;
        if(length > 0)
            templength = length;
        this.length = templength;
        this.ptr = 0;
        samples = new TrainingSample[length];
    }

    public SampleBuffer(int length){
        this(length, "");
    }

    /**
     * reads a Buffer from a file
     */
    public SampleBuffer(String filename) throws IOException {
        fullPath = BASE_PATH + filename;
        readSample();
    }

    /**
     * creates a sampleBuffer with data from the Buffer saved at fileNam
     * @param filename reads the data from here
     * @param capacity max capacity (set to the one of the file if smaller)
     */
    public SampleBuffer(String filename, int capacity) throws IOException {
        fullPath = BASE_PATH + filename;
        readSample(capacity);
    }

    public void addSample(TrainingSample s){
        samples[ptr] = s;
        ptr++;
    }


    /**
     * add a sample to the Buffer
     * @param input the flat encoded input
     * @param pi the move distribution at the root node
     * @param z the preemptive z value
     */
    public void addSample(float[] input, float[] pi, float z){
        samples[ptr] = new TrainingSample(input, pi, z);
        ptr++;
    }

    // currently unused will come into effect after fixing various move gen issues
    private static float blendZ(float term, float mcts, float progress){
        float weight = lambda * progress + (1 - lambda) * (1 - progress);
        return weight * term + (1 - weight) * mcts;
    }

    public TrainingSample sample(){
        int ind = random.nextInt(ptr);
        return samples[ind];
    }

    public int getPtr(){
        return ptr;
    }

    /**
     * update the Z value of past games while decaying the value as the moves get further away from the end
     * @param start the first game
     * @param z the z value to update with
     */
    public void updateZ(int start, float z){
        for (int i = ptr-1; i >= start; i--) {
            samples[i].z = z - ((z < 0)? -DECAY : DECAY);
            z = -z;
        }
    }

    private void readSample(int gLength) throws IOException {
        try (DataInputStream is = new DataInputStream(
                new BufferedInputStream(new FileInputStream(fullPath + ".bin")))) {

            length = Math.max(is.readInt(), gLength);
            ptr = is.readInt();

            samples = new TrainingSample[length];


            int totalFloats = TENSOR_SIZE + MOVE_POSSIBILITIES + 1;
            byte[] byteBuffer = new byte[4 * totalFloats];
            ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);
            buffer.order(ByteOrder.BIG_ENDIAN);

            for (int i = 0; i < ptr; i++) {
                is.readFully(byteBuffer);
                buffer.rewind();

                float[] tensor = new float[TENSOR_SIZE];
                float[] pi = new float[MOVE_POSSIBILITIES];

                for (int j = 0; j < TENSOR_SIZE; j++) {
                    tensor[j] = buffer.getFloat();
                }

                for (int j = 0; j < MOVE_POSSIBILITIES; j++) {
                    pi[j] = buffer.getFloat();
                }

                float z = buffer.getFloat();

                samples[i] = new TrainingSample(tensor, pi, z);
            }
        }
    }

    private void readSample() throws IOException {
        readSample(0);
    }

    public void writeSamples() throws IOException {
        try (DataOutputStream os = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(fullPath + ".bin")))) {

            os.writeInt(length);
            os.writeInt(ptr);

            ByteBuffer buffer = ByteBuffer.allocate(4 * (TENSOR_SIZE + MOVE_POSSIBILITIES + 1));
            buffer.order(ByteOrder.BIG_ENDIAN); // match DataOutputStream

            for (int i = 0; i < ptr; i++) {
                TrainingSample s = samples[i];

                buffer.clear();

                // tensor
                for (float v : s.tensor) {
                    buffer.putFloat(v);
                }

                // pi
                for (float v : s.pi) {
                    buffer.putFloat(v);
                }

                // z
                buffer.putFloat(s.z);

                os.write(buffer.array());
            }

            os.flush();
        }
    }


    public static final class TrainingSample {
        public final float[] tensor;
        public final float[] pi;
        public  float z;

        public TrainingSample(float[] tensor, float[] pi, float z){
            this.tensor = tensor;
            this.pi = pi;
            this.z = z;
        }

    }
}
