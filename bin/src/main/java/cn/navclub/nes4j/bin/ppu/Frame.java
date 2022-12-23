package cn.navclub.nes4j.bin.ppu;

import java.util.Arrays;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class Frame {
    public static final int width = 256;
    public static final int height = 240;

    private final int[] pixels;

    public Frame() {
        this.pixels = new int[width * height];
    }

    public final int getPixel(int pos) {
        return this.pixels[pos] | (0xff << 24);
    }

    public void update(int x, int y, int pixel) {
        this.pixels[y * width + x] = pixel;
    }

    public void clear() {
        Arrays.fill(this.pixels, 0, this.pixels.length, (byte) 0);
    }
}
