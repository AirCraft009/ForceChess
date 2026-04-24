package org.mxnik.forcechess.network_bot_interactions.training;

import org.mxnik.forcechess.global.FileLocations;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import static org.mxnik.forcechess.network_bot_interactions.Pos.Move.MOVE_POSSIBILITIES;
import static org.mxnik.forcechess.network_bot_interactions.Pos.PositionEncoder.TENSOR_SIZE;

public class SampleBuffer {
    public int length;
    private int ptr;
    private TrainingSample[] samples;
    private final Random random = new Random();
    private final String fullPath;
    private final float DECAY = 0.1F;
    public final static String BASE_PATH = FileLocations.SAMPLE_LOCATIONS;

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

    public void addSample(float[] input, float[] pi, float z){
        samples[ptr] = new TrainingSample(input, pi, z);
        ptr++;
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
    public void updateZ(int start, float z  ){
        for (int i = ptr-1; i >= start; i--) {
            samples[i].z = z;
            z -= DECAY;
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



}
