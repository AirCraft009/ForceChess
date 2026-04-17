package org.mxnik.forcechess.engine.network;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.mxnik.forcechess.engine.Pos.Move;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import static org.mxnik.forcechess.engine.Pos.Move.MOVE_POSSIBILITIES;
import static org.mxnik.forcechess.engine.Pos.PositionEncoder.TENSOR_SIZE;

public class SampleBuffer {
    public int length;
    private int ptr;
    private TrainingSample[] samples;
    private final Random random = new Random();
    private final String fullPath;
    public final static String BASE_PATH = "boardsNBots/bots/sample_data/";

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

    public SampleBuffer(String filename) throws IOException {
        fullPath = BASE_PATH + filename;
        samples = new TrainingSample[length];
        readSample();
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

    public void updateZ(int start, float z  ){
        for (int i = start; i < ptr; i++) {
            samples[i].z = z;
        }
    }

    private void readSample() throws IOException {
        try (DataInputStream is = new DataInputStream(
                new BufferedInputStream(new FileInputStream(fullPath + ".bin")))) {

            length = is.readInt();
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
