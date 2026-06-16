package org.mxnik.forcechess.Training;

import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Pos.PositionEncoder;

public class MixedBuffer implements TrainingsBuffer{

    private final TrainingsBuffer[] internalBuffers;
    public final int bufferCount;
    private int bufferPtr = 0;

    public MixedBuffer(TrainingsBuffer ...buffers){
        if(buffers.length < 2){
            throw new IllegalArgumentException("At least one Buffer has to be passed to the mixed Buffer");
        }
        internalBuffers = buffers;
        bufferCount = buffers.length;
    }



    @Override
    public DiversePair<SampleBuffer.TrainingSample, PositionEncoder.Position> getNext() {
        var actBuffer = internalBuffers[bufferPtr++];
        bufferPtr %= bufferCount;
        return actBuffer.getNext();
    }
}
