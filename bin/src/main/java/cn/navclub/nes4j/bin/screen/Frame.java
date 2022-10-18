package cn.navclub.nes4j.bin.screen;

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
        //width=32*8 height=30*8
        this(256, 240);
    }

    public void updatePixel(int x, int y, int rgb) {
        var index = y * 3 * width + x * 3;
        if (index + 2 < this.pixels.length) {
            this.pixels[index] = (byte) (rgb & 0xff);
            this.pixels[index + 1] = (byte) ((rgb >> 8) & 0xff);
            this.pixels[index + 2] = (byte) ((rgb >> 16) & 0xff);
        }
    }

    public void clear() {
        Arrays.fill(this.pixels, 0, this.pixels.length, (byte) 0);
    }
}
