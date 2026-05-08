package org.mxnik.forcechess.General;

public final class ConsoleBar {

    public static final int WIDTH = 40;

    public static void render(double progress) {
        int filled = (int) (progress * WIDTH);

        String bar = "#".repeat(filled)
                + "-".repeat(WIDTH - filled);

        System.out.printf("\r[%s] %3d%%", bar, (int) (progress * 100));
    }
}