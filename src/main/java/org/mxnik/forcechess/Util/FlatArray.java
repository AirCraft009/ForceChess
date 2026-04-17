package org.mxnik.forcechess.Util;

public class FlatArray {
    public final float[] arr;
    private final int[] dimensions;
    private final int dimCount;

    public FlatArray(int ... dimensions){
        arr = new float[getMax(dimensions)];
        this.dimensions = dimensions;
        dimCount = dimensions.length;
    }

    public float get(int ... mDimIndex){
        if (dimCount != mDimIndex.length){
            throw new IllegalArgumentException("illegal amount of dimensions in multidim. arr");
        }

        int idx = 0;
        for (int i = 0; i < mDimIndex.length; i++) {
            idx += dimensions[i] * mDimIndex[i];
        }

        return arr[idx];
    }

    public void set(float val, int ... mDimIndex){
        if (dimCount != mDimIndex.length){
            throw new IllegalArgumentException("illegal amount of dimensions in multidim. arr");
        }

        int idx = 0;
        for (int i = 0; i < mDimIndex.length; i++) {
            idx += dimensions[i] * mDimIndex[i];
        }

        arr[idx] = val;
    }

    private int getMax(int[] dimensions){
        int res = 1;
        for (int size : dimensions){
            res *= size;
        }
        return res;
    }
}
