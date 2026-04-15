package org.mxnik.forcechess.engine.network;

public record TrainingSample(float[] tensor, float[] pi, float z) {
}
