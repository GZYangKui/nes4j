package cn.navclub.nes4j.bin.ppu;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class Frame {
    public final int width;
    public final int height;
    private final byte[] pixels;

    public Frame(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new byte[width * height * 3];
    }

    public Frame() {
        this(256, 240);
    }


    public void update(int x, int y, int r, int g, int b) {
        var offset = y * 256 * 3;
        this.pixels[offset + x] = (byte) r;
        this.pixels[offset + x + 1] = (byte) g;
        this.pixels[offset + x + 2] = (byte) b;
    }

    public void clear() {
        Arrays.fill(this.pixels, 0, this.pixels.length, (byte) 0);
    }
}
