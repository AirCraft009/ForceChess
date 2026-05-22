package org.mxnik.forcechess.General;

public final class ConsoleBar {

    public static final int WIDTH = 40;

    public static void render(double progress, int precision) {
        int filled = (int) (progress * WIDTH);

        String bar = "#".repeat(filled)
                + "-".repeat(WIDTH - filled);

        String format = "\r[%s] %." + precision + "f%%";
        System.out.printf(format, bar, (float) progress * 100.0);
    }
}