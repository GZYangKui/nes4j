package cn.navclub.nes4j.bin.screen;

import cn.navclub.nes4j.bin.core.PPU;

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

    private static byte[] bgPalette(PPU ppu, byte[] atrTable, int column, int row) {
        var idx = 0;
        var test = 3;
        var a = column % 4 / 2;
        var b = row % 4 / 2;
        var attrByte = atrTable[row / 4 * 8 + column / 4] & 0xff;
        if (a == 0 && b == 0)
            idx = attrByte & test;
        else if (a == 1 && b == 0)
            idx = attrByte >> 2 & test;
        else if (a == 0 && b == 1)
            idx = attrByte >> 4 & test;
        else if (a == 1 && b == 1)
            idx = attrByte >> 6 & test;
        else
            throw new RuntimeException("Unknown background palette.");

        var offset = 1 + idx * 4;

        return new byte[]{
                ppu.getPaletteTable()[0],
                ppu.getPaletteTable()[offset],
                ppu.getPaletteTable()[offset + 1],
                ppu.getPaletteTable()[offset + 2]
        };
    }

    public static void render(PPU ppu, Frame frame) {
        var vram = ppu.getVram();
        var mirror = ppu.getMirrors();

        var ctr = ppu.getControl();
        var nameTable = ctr.nameTableAddr();

        var scrollX = ppu.getScroll().getX();
        var scrollY = ppu.getScroll().getY();

        var firstNameTable = new byte[0x400];
        var secondNameTable = new byte[0x400];

        if ((mirror == 1 && (nameTable == 0x2000 || nameTable == 0x2800))
                || (mirror == 0 && (nameTable == 0x2000 || nameTable == 0x2400))) {
            System.arraycopy(vram, 0, firstNameTable, 0, 0x400);
            System.arraycopy(vram, 0x400, secondNameTable, 0, 0x400);
        } else if ((mirror == 1 && (nameTable == 0x2400 || nameTable == 0x2c00))
                || (mirror == 0 && (nameTable == 0x2800 || nameTable == 0x2c00))) {
            System.arraycopy(vram, 0x400, firstNameTable, 0, 0x400);
            System.arraycopy(vram, 0, secondNameTable, 0, 0x400);
        } else {
            throw new RuntimeException("Not support mirror type:" + mirror);
        }

        //Render background 960 tile
        renderNameTable(ppu, frame, firstNameTable, new Rect(scrollX, scrollY, 256, 240), -scrollX, -scrollY);

        if (scrollX > 0) {
            renderNameTable(ppu,
                    frame, secondNameTable, new Rect(0, 0, scrollX, 240), 256 - scrollX, 0);
        } else if (scrollY > 0) {
            renderNameTable(ppu,
                    frame, secondNameTable, new Rect(0, 0, 256, scrollY), 0, 240 - scrollY);
        }


        var oam = ppu.getOam();
        var length = oam.length;
        for (int i = length - 4; i >= 0; i = i - 4) {
            var idx = oam[i + 1] & 0xff;
            var tx = oam[i + 3] & 0xff;
            var ty = oam[i] & 0xff;

            var vFlip = ((oam[i + 2] & 0xff) >> 7 & 1) == 1;
            var hFlip = ((oam[i + 2] & 0xff) >> 6 & 1) == 1;

            var pIdx = (oam[i + 2] & 0xff) & 0b11;

            var sp = spritePalette(ppu, pIdx);
            var bank = ppu.getControl().spritePattern();

            var tile = new byte[16];
            System.arraycopy(ppu.getCh(), bank + idx * 16, tile, 0, 16);

            for (int y = 0; y < 8; y++) {
                var upper = tile[y] & 0xff;
                var lower = tile[y + 8] & 0xff;
                for (int x = 7; x >= 0; x--) {
                    var value = (1 & lower) << 1 | (1 & upper);
                    upper >>= 1;
                    lower >>= 1;
                    var rgb = switch (value) {
                        case 1 -> SYS_PALETTE[sp[1]];
                        case 2 -> SYS_PALETTE[sp[2]];
                        case 3 -> SYS_PALETTE[sp[3]];
                        default -> new int[0];
                    };
                    if (rgb.length == 0) {
                        continue;
                    }

                    if (!hFlip && !vFlip) {
                        frame.updatePixel(tx + x, ty + y, rgb);
                    }
                    if (hFlip && !vFlip) {
                        frame.updatePixel(tx + 7 - x, ty + y, rgb);
                    }
                    if (!hFlip && vFlip) {
                        frame.updatePixel(tx + x, ty + 7 - y, rgb);
                    }
                    if (hFlip && vFlip) {
                        frame.updatePixel(tx + 7 - x, ty + 7 - y, rgb);
                    }
                }
            }
        }
    }

    private static byte[] spritePalette(PPU ppu, int idx) {
        var offset = 0x11 + idx * 4;
        return new byte[]{
                0,
                ppu.getPaletteTable()[offset],
                ppu.getPaletteTable()[offset + 1],
                ppu.getPaletteTable()[offset + 2]
        };
    }

    private static void renderNameTable(PPU ppu, Frame frame, byte[] nameTable, Rect viewPort, int shiftX, int shiftY) {

        var attrTable = new byte[64];
        System.arraycopy(nameTable, 0x3c0, attrTable, 0, attrTable.length);

        var bank = ppu.getControl().bkNamePatternTable();

        //渲染背景960个tile
        for (int i = 0; i < 0x3c0; i++) {
            var row = i / 32;
            var column = i % 32;
            var idx = nameTable[i] & 0xff;
            var tile = new byte[16];
            var offset = bank + idx * 16;
            System.arraycopy(ppu.getCh(), offset, tile, 0, 16);
            var palette = bgPalette(ppu, attrTable, column, row);
            for (int y = 0; y < 8; y++) {
                var upper = tile[y] & 0xff;
                var lower = tile[y + 8] & 0xff;
                for (int x = 7; x >= 0; x--) {
                    var value = ((1 & lower) << 1) | (1 & upper);
                    upper >>= 1;
                    lower >>= 1;
                    var rgb = switch (value) {
                        case 0 -> SYS_PALETTE[ppu.getPaletteTable()[0]];
                        case 1 -> SYS_PALETTE[palette[1]];
                        case 2 -> SYS_PALETTE[palette[2]];
                        case 3 -> SYS_PALETTE[palette[3]];
                        //throw exception?
                        default -> new int[]{0, 0, 0};
                    };
                    var pixelX = column * 8 + x;
                    var pixelY = row * 8 + y;

                    //判断是否显示范围
                    if (pixelX >= viewPort.tx() && pixelX < viewPort.bx() && pixelY >= viewPort.ty() && pixelY < viewPort.by()) {
                        frame.updatePixel(shiftX + pixelX, shiftY + pixelY, rgb);
                    }
                }
            }
        }
    }
}
