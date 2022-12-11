package cn.navclub.nes4j.bin.ppu;

import lombok.Getter;

import java.util.Arrays;

public class Frame {
    @Getter
    public final int width;
    @Getter
    public final int height;
    private final int[] pixels;

    public Frame(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }


    public Frame() {
        this(256, 240);
    }

    public final int getPixel(int pos) {
        var pixel = this.pixels[pos];
        if (pixel != 0) {
            pixel |= (0xff << 24);
        }
        return pixel;
    }

    public void update(int x, int y, int pixel) {
        this.pixels[y * width + x] = pixel;
    }

    public void clear() {
        Arrays.fill(this.pixels, 0, this.pixels.length, (byte) 0);
    }
}
