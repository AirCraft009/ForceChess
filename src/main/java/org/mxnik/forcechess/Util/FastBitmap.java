package org.mxnik.forcechess.Util;

public final class FastBitmap {

    private final long[] words;

    public FastBitmap(int bits) {
        words = new long[(bits + 63) >>> 6];
    }

    public void set(int bit) {
        words[bit >>> 6] |= 1L << bit;
    }

    public void clear(int bit) {
        words[bit >>> 6] &= ~(1L << bit);
    }

    public long get(int bit) {
        return (words[bit >>> 6] & (1L << bit));
    }
}
