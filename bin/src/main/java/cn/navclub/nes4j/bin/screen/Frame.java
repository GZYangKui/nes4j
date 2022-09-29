package cn.navclub.nes4j.bin.screen;

import java.util.Arrays;

public class Frame {
    private final static int WIDTH = 250;
    private final static int HEIGHT = 240;

    private final byte[] buffer;

    public Frame() {
        this.buffer = new byte[WIDTH * HEIGHT * 3];
    }


    public void updatePixel(int x, int y, byte r, byte g, byte b) {
        var index = y * 3 * WIDTH + x * 3;
        if (index + 2 < this.buffer.length) {
            this.buffer[index] = r;
            this.buffer[index + 1] = g;
            this.buffer[index + 2] = b;
        }
    }

    public void clear() {
        Arrays.fill(this.buffer, 0, this.buffer.length, (byte) 0);
    }
}
