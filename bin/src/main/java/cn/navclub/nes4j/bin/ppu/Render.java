//package cn.navclub.nes4j.bin.ppu;
//
//import cn.navclub.nes4j.bin.config.MaskFlag;
//import cn.navclub.nes4j.bin.util.PPUUtil;
//import lombok.Getter;
//
//import static cn.navclub.nes4j.bin.util.PPUUtil.bgPalette;
//
//public class Render {
//    private static final int[][] DEF_SYS_PALETTE;
//
//    static {
//        DEF_SYS_PALETTE = new int[][]{
//                {0x80, 0x80, 0x80}, {0x00, 0x3D, 0xA6}, {0x00, 0x12, 0xB0}, {0x44, 0x00, 0x96}, {0xA1, 0x00, 0x5E},
//                {0xC7, 0x00, 0x28}, {0xBA, 0x06, 0x00}, {0x8C, 0x17, 0x00}, {0x5C, 0x2F, 0x00}, {0x10, 0x45, 0x00},
//                {0x05, 0x4A, 0x00}, {0x00, 0x47, 0x2E}, {0x00, 0x41, 0x66}, {0x00, 0x00, 0x00}, {0x05, 0x05, 0x05},
//                {0x05, 0x05, 0x05}, {0xC7, 0xC7, 0xC7}, {0x00, 0x77, 0xFF}, {0x21, 0x55, 0xFF}, {0x82, 0x37, 0xFA},
//                {0xEB, 0x2F, 0xB5}, {0xFF, 0x29, 0x50}, {0xFF, 0x22, 0x00}, {0xD6, 0x32, 0x00}, {0xC4, 0x62, 0x00},
//                {0x35, 0x80, 0x00}, {0x05, 0x8F, 0x00}, {0x00, 0x8A, 0x55}, {0x00, 0x99, 0xCC}, {0x21, 0x21, 0x21},
//                {0x09, 0x09, 0x09}, {0x09, 0x09, 0x09}, {0xFF, 0xFF, 0xFF}, {0x0F, 0xD7, 0xFF}, {0x69, 0xA2, 0xFF},
//                {0xD4, 0x80, 0xFF}, {0xFF, 0x45, 0xF3}, {0xFF, 0x61, 0x8B}, {0xFF, 0x88, 0x33}, {0xFF, 0x9C, 0x12},
//                {0xFA, 0xBC, 0x20}, {0x9F, 0xE3, 0x0E}, {0x2B, 0xF0, 0x35}, {0x0C, 0xF0, 0xA4}, {0x05, 0xFB, 0xFF},
//                {0x5E, 0x5E, 0x5E}, {0x0D, 0x0D, 0x0D}, {0x0D, 0x0D, 0x0D}, {0xFF, 0xFF, 0xFF}, {0xA6, 0xFC, 0xFF},
//                {0xB3, 0xEC, 0xFF}, {0xDA, 0xAB, 0xEB}, {0xFF, 0xA8, 0xF9}, {0xFF, 0xAB, 0xB3}, {0xFF, 0xD2, 0xB0},
//                {0xFF, 0xEF, 0xA6}, {0xFF, 0xF7, 0x9C}, {0xD7, 0xE8, 0x95}, {0xA6, 0xED, 0xAF}, {0xA2, 0xF2, 0xDA},
//                {0x99, 0xFF, 0xFC}, {0xDD, 0xDD, 0xDD}, {0x11, 0x11, 0x11}, {0x11, 0x11, 0x11}
//        };
//    }
//
//    @Getter
//    private final int[][] sysPalette;
//
//    public Render() {
//        this.sysPalette = DEF_SYS_PALETTE;
//    }
//
//
//    public void render(PPU ppu, Frame frame) {
//        var mask = ppu.getMask();
//        var sprite = mask.contain(MaskFlag.SHOW_SPRITES);
//        var background = mask.contain(MaskFlag.SHOW_BACKGROUND);
//        //Render background
//        if (background) {
//
//            var scrollX = ppu.getScroll().getX();
//            var scrollY = ppu.getScroll().getY();
//
//            var firstNameTable = new byte[0x400];
//            var secondNameTable = new byte[0x400];
//
//            //Fill fist and second name table
//            PPUUtil.fillNameTable(ppu, firstNameTable, secondNameTable);
//
//
//            //Render visible area left/top portion
//            renderNameTable(ppu, frame, firstNameTable, new Camera(scrollX, scrollY, 256, 240), -scrollX, -scrollY);
//
//            //Render visible area right/bottom portion
//            if (scrollX > 0) {
//                renderNameTable(ppu,
//                        frame, secondNameTable, new Camera(0, 0, scrollX, 240), 256 - scrollX, 0);
//            } else if (scrollY > 0) {
//                renderNameTable(ppu,
//                        frame, secondNameTable, new Camera(0, 0, 256, scrollY), 0, 240 - scrollY);
//            }
//
//        }
//
//        //Render sprite
//        if (sprite) {
//            var oam = ppu.getOam();
//            var length = oam.length;
//            for (int i = length - 4; i >= 0; i = i - 4) {
//                //Y position of top of sprite
//                var ty = oam[i] & 0xff;
//                //Tile index number
//                var idx = oam[i + 1] & 0xff;
//                //X position of left side of sprite.
//                var tx = oam[i + 3] & 0xff;
//
//
//                //
//                //76543210
//                //||||||||
//                //||||||++- Palette (4 to 7) of sprite
//                //|||+++--- Unimplemented (read 0)
//                //||+------ Priority (0: in front of background; 1: behind background)
//                //|+------- Flip sprite horizontally
//                //+-------- Flip sprite vertically
//                //
//                //
//                var attr = oam[i + 2] & 0xff;
//
//                var pIdx = attr & 0x03;
//                var vFlip = (attr >> 7 & 1) == 1;
//                var hFlip = (attr >> 6 & 1) == 1;
//
//                var sp = spritePalette(ppu, pIdx);
//
//
//                var ctrl = ppu.getControl();
//                var size = ctrl.spriteSize();
//                var tile = new byte[16];
//
//                var bank = size == 0x08 ? ctrl.spritePattern8() : ctrl.spritePattern16(idx);
//
//                if (size == 0x10) {
//                    idx = idx & 0xfe;
//                }
//
//                //
//                // Forum discuss about sprite size in 8*16 <a href="https://forums.nesdev.org/viewtopic.php?t=6194">detail</a>.
//                //
//                for (int k = 8; k <= size; k += 8) {
//                    System.arraycopy(ppu.getCh(), bank + idx * 16, tile, 0, 16);
//                    for (int y = 0; y < 8; y++) {
//                        var left = tile[y % 8] & 0xff;
//                        var right = tile[y + 8] & 0xff;
//                        //
//                        // The character is constructed pixel by pixel by taking one bit from the top left and
//                        // one from the top right to make a 2-bit colour. The other two bits of the colour are taken from
//                        // the attribute tables. The colours shown are not genuine NES colour palette values.
//                        //
//                        for (int x = 7; x >= 0; x--) {
//                            var value = (((1 & right) << 1) | (1 & left));
//                            left >>= 1;
//                            right >>= 1;
//                            var rgb = switch (value) {
//                                case 1 -> sysPalette[sp[1]];
//                                case 2 -> sysPalette[sp[2]];
//                                case 3 -> sysPalette[sp[3]];
//                                default -> new int[0];
//                            };
//
//                            if (rgb.length == 0) {
//                                continue;
//                            }
//
//                            if (!hFlip && !vFlip) {
//                                frame.updatePixel(tx + x, ty + y, rgb);
//                            }
//                            if (hFlip && !vFlip) {
//                                frame.updatePixel(tx + 7 - x, ty + y, rgb);
//                            }
//                            if (!hFlip && vFlip) {
//                                frame.updatePixel(tx + x, ty + 7 - y, rgb);
//                            }
//                            if (hFlip && vFlip) {
//                                frame.updatePixel(tx + 7 - x, ty + 7 - y, rgb);
//                            }
//                        }
//                    }
//                    ty += 8;
//                    idx += 1;
//                }
//            }
//        }
//    }
//
//    private static byte[] spritePalette(PPU ppu, int idx) {
//        var offset = 0x11 + idx * 4;
//        return new byte[]{
//                0,
//                ppu.getPaletteTable()[offset],
//                ppu.getPaletteTable()[offset + 1],
//                ppu.getPaletteTable()[offset + 2]
//        };
//    }
//
//    private void renderNameTable(PPU ppu, Frame frame, byte[] nameTable, Camera camera, int sx, int sy) {
//
//        var attrTable = new byte[64];
//        System.arraycopy(nameTable, 0x3c0, attrTable, 0, attrTable.length);
//
//        var bank = ppu.getControl().bkNamePatternTable();
//
//        var tile = new byte[16];
//
//        //渲染背景32*30=960个tile
//        for (int i = 0; i < 0x3c0; i++) {
//            var row = i / 32;
//            var column = i % 32;
//            var idx = nameTable[i] & 0xff;
//            var offset = bank + idx * 16;
//            System.arraycopy(ppu.getCh(), offset, tile, 0, 16);
//            var palette = bgPalette(ppu, attrTable, column, row);
//            for (int y = 0; y < 8; y++) {
//                var left = tile[y] & 0xff;
//                var right = tile[y + 8] & 0xff;
//                for (int x = 7; x >= 0; x--) {
//                    var value = ((1 & right) << 1) | (1 & left);
//                    left >>= 1;
//                    right >>= 1;
//                    var rgb = switch (value) {
//                        case 0 -> sysPalette[palette[0]];
//                        case 1 -> sysPalette[palette[1]];
//                        case 2 -> sysPalette[palette[2]];
//                        case 3 -> sysPalette[palette[3]];
//                        //throw exception?
//                        default -> new int[]{0, 0, 0};
//                    };
//
//                    var py = row * 8 + y;
//                    var px = column * 8 + x;
//
//                    //判断是否相机显示范围
//                    if (px >= camera.x0() && px < camera.x1() && py >= camera.y0() && py < camera.y1()) {
//                        frame.updatePixel(sx + px, sy + py, rgb);
//                    }
//                }
//            }
//        }
//    }
//
//
//}
