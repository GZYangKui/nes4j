package cn.navclub.nes4j.bin.screen;

import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.util.PatternTableUtil;

public class Render {
    private static final int[][] SYS_PALETTE;

    static {
        SYS_PALETTE = new int[][]{
                {0x80, 0x80, 0x80}, {0x00, 0x3D, 0xA6}, {0x00, 0x12, 0xB0}, {0x44, 0x00, 0x96}, {0xA1, 0x00, 0x5E},
                {0xC7, 0x00, 0x28}, {0xBA, 0x06, 0x00}, {0x8C, 0x17, 0x00}, {0x5C, 0x2F, 0x00}, {0x10, 0x45, 0x00},
                {0x05, 0x4A, 0x00}, {0x00, 0x47, 0x2E}, {0x00, 0x41, 0x66}, {0x00, 0x00, 0x00}, {0x05, 0x05, 0x05},
                {0x05, 0x05, 0x05}, {0xC7, 0xC7, 0xC7}, {0x00, 0x77, 0xFF}, {0x21, 0x55, 0xFF}, {0x82, 0x37, 0xFA},
                {0xEB, 0x2F, 0xB5}, {0xFF, 0x29, 0x50}, {0xFF, 0x22, 0x00}, {0xD6, 0x32, 0x00}, {0xC4, 0x62, 0x00},
                {0x35, 0x80, 0x00}, {0x05, 0x8F, 0x00}, {0x00, 0x8A, 0x55}, {0x00, 0x99, 0xCC}, {0x21, 0x21, 0x21},
                {0x09, 0x09, 0x09}, {0x09, 0x09, 0x09}, {0xFF, 0xFF, 0xFF}, {0x0F, 0xD7, 0xFF}, {0x69, 0xA2, 0xFF},
                {0xD4, 0x80, 0xFF}, {0xFF, 0x45, 0xF3}, {0xFF, 0x61, 0x8B}, {0xFF, 0x88, 0x33}, {0xFF, 0x9C, 0x12},
                {0xFA, 0xBC, 0x20}, {0x9F, 0xE3, 0x0E}, {0x2B, 0xF0, 0x35}, {0x0C, 0xF0, 0xA4}, {0x05, 0xFB, 0xFF},
                {0x5E, 0x5E, 0x5E}, {0x0D, 0x0D, 0x0D}, {0x0D, 0x0D, 0x0D}, {0xFF, 0xFF, 0xFF}, {0xA6, 0xFC, 0xFF},
                {0xB3, 0xEC, 0xFF}, {0xDA, 0xAB, 0xEB}, {0xFF, 0xA8, 0xF9}, {0xFF, 0xAB, 0xB3}, {0xFF, 0xD2, 0xB0},
                {0xFF, 0xEF, 0xA6}, {0xFF, 0xF7, 0x9C}, {0xD7, 0xE8, 0x95}, {0xA6, 0xED, 0xAF}, {0xA2, 0xF2, 0xDA},
                {0x99, 0xFF, 0xFC}, {0xDD, 0xDD, 0xDD}, {0x11, 0x11, 0x11}, {0x11, 0x11, 0x11}
        };
    }

    /**
     * <a href="https://www.nesdev.org/wiki/PPU_attribute_tables">PPU attribute tables</a>
     */

    private static byte[] bgPalette(PPU ppu, int column, int row) {
        var idx = row / 4 * 8 + column / 4;
        var attrByte = ppu.getVram()[idx + 0x3c0];
        var paletteIdx = 0;
        var a = column % 4 / 2;
        var b = row % 4 / 2;
        if (a == 0 && b == 0)
            paletteIdx = attrByte & 0x11;
        else if (a == 1 && b == 0)
            paletteIdx = attrByte >> 2 & 0b11;
        else if (a == 0 && b == 1)
            paletteIdx = attrByte >> 4 & 0b11;
        else if (a == 1 && b == 1)
            paletteIdx = attrByte >> 6 & 0b11;
        var offset = 1 + paletteIdx * 4;

        return new byte[]{
                ppu.getPaletteTable()[0],
                ppu.getPaletteTable()[offset],
                ppu.getPaletteTable()[offset + 1],
                ppu.getPaletteTable()[offset + 2]
        };
    }

    public static void render(PPU ppu, Frame frame) {
        var ctr = ppu.getControl();
        var index = ctr.patternNameAddr();
        var nameTable = ctr.nameTableAddr();
        //读取960个tile背景
        for (int i = 0; i < 960; i++) {
            var x = i % 32;
            var y = i / 32;
            var tile = new byte[16];
            var vram = ppu.getVram();
            var palette = bgPalette(ppu, x, y);
            //获取tile编号
            var numbered = vram[i] & 0xff;
            System.arraycopy(ppu.getCh(), index + numbered * 16, tile, 0, 16);
            var arr = PatternTableUtil.tiles(tile);
            for (int h = 0; h < arr.length; h++) {
                var row = arr[h];
                for (int k = 0; k < row.length; k++) {
                    var rgb = switch (k) {
                        case 1 -> SYS_PALETTE[palette[1]];
                        case 2 -> SYS_PALETTE[palette[2]];
                        default -> SYS_PALETTE[palette[0]];
                    };
                    frame.updatePixel(x * 8 + k, y * 8 + h, rgb);
                }
            }
        }
    }

    private static int[] paletteStr2Arr(String paletteStr, char skip) {
        var append = false;
        var index = 0;
        var counter = 0;
        var chr = new byte[3];
        var target = new int[512];
        var arr = paletteStr.getBytes();
        for (byte b : arr) {
            append = (b != skip && b != '\n');
            if (!append) {
                if (index != 0) {
                    var str = new String(chr, 0, index);
                    target[counter++] = Integer.parseInt(str);
                }
                index = 0;
                continue;
            }
            chr[index++] = b;
        }
        var dst = new int[counter];
        System.arraycopy(target, 0, dst, 0, counter);
        return dst;
    }
}
