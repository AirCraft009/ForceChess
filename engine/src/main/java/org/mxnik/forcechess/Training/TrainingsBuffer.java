package org.mxnik.forcechess.Training;

import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Pos.PositionEncoder;

import java.io.IOException;

public interface TrainingsBuffer {
    public DiversePair<SampleBuffer.TrainingSample, PositionEncoder.Position> getNext();
}
