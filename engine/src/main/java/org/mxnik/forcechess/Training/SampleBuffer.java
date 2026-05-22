package org.mxnik.forcechess.Training;

import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.General.FileLocations;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.mxnik.forcechess.Pos.Move.MOVE_POSSIBILITIES;
import static org.mxnik.forcechess.Pos.PositionEncoder.TENSOR_SIZE;

public class SampleBuffer implements TrainingsBuffer{
    public int length;
    private int ptr;
    private TrainingSample[] samples;
    private final Random random = new Random();
    private final String fullPath;
    private final float DECAY = 0.0F;
    public final static String BASE_PATH = FileLocations.SAMPLE_LOCATIONS;
    private final static float lambda = 0.8F;
    private int samplePtr = 0;

    /**
     * creates a sample buffer with a given capacity;
     * @param filename name used to manage the writing and reading data to file
     */
    public SampleBuffer(int length, String filename, boolean read) throws IOException {
        fullPath = BASE_PATH + filename;
        int templength = 1;
        if(length > 0)
            templength = length;
        this.length = templength;
        this.ptr = 0;
        if(read){
            readSampleChunk(0, Integer.MAX_VALUE, length);
        }else {
            samples = new TrainingSample[length];
        }
    }

    /**
     * reads a Buffer from a file
     */
    public SampleBuffer(String filename) throws IOException {
        fullPath = BASE_PATH + filename;
        readSample();
    }

    public void addSample(TrainingSample s){
        samples[ptr] = s;
        ptr++;
    }

    public void shuffel(){
        for (int i = 0; i < ptr; i++) {
            int newInd = random.nextInt(0, ptr);
            TrainingSample p = samples[i];
            samples[i] = samples[newInd];
            samples[newInd] = p;
        }
    }

    /**
     * writes all current samples to the file and resets samples as well as ptr
     * @param append append to file or overwrite
     */
    public void flushToFile(boolean append) throws IOException {
        writeSamples(append);
        samples = new TrainingSample[length];
        ptr = 0;
    }

    public void resetSamples(){
        samplePtr = 0;
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

    /**
     * returns a random value from the array
     */
    public TrainingSample sample(){
        int ind = random.nextInt(ptr);
        return samples[ind];
    }

    /**
     * get Trainingset at sample
     */
    public TrainingSample sample(int ind){
        return samples[ind];
    }

    public DiversePair<TrainingSample, PositionEncoder.Position> getNext(){
        return new DiversePair<>(samples[samplePtr++ % ptr], PositionEncoder.Position.emptyPosition());
    }


    public int getPtr(){
        return ptr;
    }

    public void setPtr(int ptr){
        this.ptr = ptr;
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

    public void combineBuffers(SampleBuffer b){
        samples = Arrays.copyOf(samples, length + b.length);
        if (b.ptr >= 0) System.arraycopy(b.samples, 0, samples, ptr, b.ptr);
    }

    void readSampleChunk(int offset, int amount, int gLength) throws IOException {
        try (DataInputStream is = new DataInputStream(
                new BufferedInputStream(new FileInputStream(fullPath + ".bin")))) {

            length = Math.clamp(gLength, amount, is.readInt());
            ptr = is.readInt();

            // Clamp range
            int start = Math.max(0, offset);
            int end = Math.min(ptr, start + amount);

            int totalFloats = TENSOR_SIZE + MOVE_POSSIBILITIES + 1;
            int sampleBytes = totalFloats * 4;

            // Skip samples before offset
            long bytesToSkip = (long) start * sampleBytes;
            long skipped = 0;

            while (skipped < bytesToSkip) {
                long s = is.skip(bytesToSkip - skipped);
                if (s <= 0) {
                    throw new EOFException("Unable to skip to offset");
                }
                skipped += s;
            }

            samples = new TrainingSample[end - start];

            byte[] byteBuffer = new byte[sampleBytes];
            ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);
            buffer.order(ByteOrder.BIG_ENDIAN);

            int loaded = 0;

            try {
                for (int i = start; i < end; i++) {
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

                    samples[loaded++] = new TrainingSample(tensor, pi, z);
                }

                ptr = loaded;

            } catch (EOFException ignored) {
                System.err.println("EOF");
                ptr = loaded;
            }
        }
    }

    private void readSample() throws IOException {
        readSampleChunk(0, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void writeSamples(boolean append) throws IOException {
        try (DataOutputStream os = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(fullPath + ".bin", append)))) {

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

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("TrainingSample{");
            sb.append("tensor=").append(Arrays.toString(tensor));
            sb.append(", pi=").append(Arrays.toString(pi));
            sb.append(", z=").append(z);
            sb.append('}');
            return sb.toString();
        }
    }
}
