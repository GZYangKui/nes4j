package cn.navclub.nes4j.bin.screen;

import java.util.Arrays;

public class Frame {
    private final static int WIDTH = 250;
    private final static int HEIGHT = 240;

    private final byte[] pixles;

    public Frame() {
        this.pixles = new byte[WIDTH * HEIGHT * 3];
    }


    public void updatePixel(int x, int y, int rgb) {
        var index = y * 3 * WIDTH + x * 3;
        if (index + 2 < this.pixles.length) {
            this.pixles[index] = (byte) (rgb & 0xff);
            this.pixles[index + 1] = (byte) ((rgb >> 8) & 0xff);
            this.pixles[index + 2] = (byte) ((rgb >> 16) & 0xff);
        }
    }

    public void clear() {
        Arrays.fill(this.pixles, 0, this.pixles.length, (byte) 0);
    }
}
