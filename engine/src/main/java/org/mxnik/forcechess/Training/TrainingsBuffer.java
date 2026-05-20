package org.mxnik.forcechess.Training;

import java.io.IOException;

public interface TrainingsBuffer {
    public SampleBuffer.TrainingSample getNext();
}
