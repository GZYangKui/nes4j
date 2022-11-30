package cn.navclub.nes4j.bin.screen;

import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.core.impl.CTRegister;
import cn.navclub.nes4j.bin.enums.MaskFlag;
import cn.navclub.nes4j.bin.enums.NameMirror;
import lombok.Getter;

public class Render {
    private static final int[][] DEF_SYS_PALETTE;

    static {
        DEF_SYS_PALETTE = new int[][]{
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

    @Getter
    private final int[][] sysPalette;

    public Render() {
        this.sysPalette = DEF_SYS_PALETTE;
    }

    /**
     * <a href="https://www.nesdev.org/wiki/PPU_attribute_tables">PPU attribute tables</a>
     */

    public byte[] bgPalette(PPU ppu, byte[] atrTable, int column, int row) {
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

    public void render(PPU ppu, Frame frame) {
        var mask = ppu.getMask();
        var sprite = mask.contain(MaskFlag.SHOW_SPRITES);
        var background = mask.contain(MaskFlag.SHOW_BACKGROUND);
        //Render background
        if (background) {
            var vram = ppu.getVram();
            var mirror = ppu.getMirrors();

            var ctr = ppu.getControl();
            var nameTable = ctr.nameTableAddr();

            var scrollX = ppu.getScroll().getX();
            var scrollY = ppu.getScroll().getY();

            var firstNameTable = new byte[0x400];
            var secondNameTable = new byte[0x400];

            if ((mirror == NameMirror.VERTICAL && (nameTable == 0x2000 || nameTable == 0x2800))
                    || (mirror == NameMirror.HORIZONTAL && (nameTable == 0x2000 || nameTable == 0x2400))) {
                System.arraycopy(vram, 0, firstNameTable, 0, 0x400);
                System.arraycopy(vram, 0x400, secondNameTable, 0, 0x400);
            } else if ((mirror == NameMirror.VERTICAL && (nameTable == 0x2400 || nameTable == 0x2c00))
                    || (mirror == NameMirror.HORIZONTAL && (nameTable == 0x2800 || nameTable == 0x2c00))) {
                System.arraycopy(vram, 0x400, firstNameTable, 0, 0x400);
                System.arraycopy(vram, 0, secondNameTable, 0, 0x400);
            } else {
                throw new RuntimeException("Not support mirror type:" + mirror);
            }


            //Render first screen background
            renderNameTable(ppu, frame, firstNameTable, new Camera(scrollX, scrollY, 256, 240), -scrollX, -scrollY);

            if (scrollX > 0) {
                renderNameTable(ppu,
                        frame, secondNameTable, new Camera(0, 0, scrollX, 240), 256 - scrollX, 0);
            } else if (scrollY > 0) {
                renderNameTable(ppu,
                        frame, secondNameTable, new Camera(0, 0, 256, scrollY), 0, 240 - scrollY);
            }
        }
        //Render sprite
        if (sprite) {
            var oam = ppu.getOam();
            var length = oam.length;
            for (int i = length - 4; i >= 0; i = i - 4) {
                //Y position of top of sprite
                var ty = oam[i] & 0xff;
                //Tile index number
                var idx = oam[i + 1] & 0xff;
                //X position of left side of sprite.
                var tx = oam[i + 3] & 0xff;


                //
                //76543210
                //||||||||
                //||||||++- Palette (4 to 7) of sprite
                //|||+++--- Unimplemented (read 0)
                //||+------ Priority (0: in front of background; 1: behind background)
                //|+------- Flip sprite horizontally
                //+-------- Flip sprite vertically
                //
                //
                var attr = oam[i + 2] & 0xff;

                var pIdx = attr & 0x03;
                var vFlip = (attr >> 7 & 1) == 1;
                var hFlip = (attr >> 6 & 1) == 1;
                var front = (attr & 0x20) >> 5 == 1;

                var sp = spritePalette(ppu, pIdx);


                var tile = new byte[16];
                var ctrl = ppu.getControl();
                final int bank;

                if (ctrl.spriteSize() == 0x08)
                    bank = ctrl.spritePattern8();
                else
                    bank = ctrl.spritePattern16(idx);

                System.arraycopy(ppu.getCh(), bank + idx * 16, tile, 0, 16);

                for (int y = 0; y < 8; y++) {
                    var upper = tile[y] & 0xff;
                    var lower = tile[y + 8] & 0xff;
                    for (int x = 7; x >= 0; x--) {
                        var value = (1 & lower) << 1 | (1 & upper);
                        upper >>= 1;
                        lower >>= 1;
                        var rgb = switch (value) {
                            case 1 -> sysPalette[sp[1]];
                            case 2 -> sysPalette[sp[2]];
                            case 3 -> sysPalette[sp[3]];
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

    private void renderNameTable(PPU ppu, Frame frame, byte[] nameTable, Camera camera, int sx, int sy) {

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
                var upper = Byte.toUnsignedInt(tile[y]);
                var lower = Byte.toUnsignedInt(tile[y + 8]);
                for (int x = 7; x >= 0; x--) {
                    var value = ((1 & lower) << 1) | (1 & upper);
                    upper >>= 1;
                    lower >>= 1;
                    var rgb = switch (value) {
                        case 0 -> sysPalette[ppu.getPaletteTable()[0]];
                        case 1 -> sysPalette[palette[1]];
                        case 2 -> sysPalette[palette[2]];
                        case 3 -> sysPalette[palette[3]];
                        //throw exception?
                        default -> new int[]{0, 0, 0};
                    };

                    var py = row * 8 + y;
                    var px = column * 8 + x;

                    //判断是否显示范围
                    if (px >= camera.x0() && px < camera.x1() && py >= camera.y0() && py < camera.y1()) {
                        frame.updatePixel(sx + px, sy + py, rgb);
                    }
                }
            }
        }
    }
}
