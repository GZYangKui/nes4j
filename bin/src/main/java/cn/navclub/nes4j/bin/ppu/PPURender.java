package cn.navclub.nes4j.bin.ppu;

import cn.navclub.nes4j.bin.config.MaskFlag;
import cn.navclub.nes4j.bin.config.PStatus;
import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;

import static cn.navclub.nes4j.bin.util.BinUtil.*;

/**
 *
 * <a hrep="https://www.nesdev.org/wiki/PPU_rendering">PPU Render</a>
 *
 * <p/>
 *
 * <pre>
 *                                          [BBBBBBBB] - Next tile's pattern data,
 *                                          [BBBBBBBB] - 2 bits per pixel
 *                                           ||||||||
 *                                           vvvvvvvv
 *       Serial-to-parallel - [AAAAAAAA] <- [BBBBBBBB] - Parallel-to-serial
 *          shift registers - [AAAAAAAA] <- [BBBBBBBB] - shift registers
 *                             vvvvvvvv
 *                             ||||||||           [Sprites 0..7]----+
 *                             ||||||||                             |
 * [fine_x selects a bit]---->[  Mux   ]-------------------->[Priority mux]----->[Pixel]
 *                             ||||||||
 *                             ^^^^^^^^
 *                            [PPPPPPPP] <- [1-bit latch]
 *                            [PPPPPPPP] <- [1-bit latch]
 *                                                ^
 *                                                |
 *                     [2-bit Palette Attribute for next tile (from attribute table)]
 * </pre>
 *
 */
public class PPURender implements CycleDriver {

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

    private final PPU ppu;


    private final Frame frame;
    //
    // 2 16-bit shift registers - These contain the pattern table data for two tiles.
    // Every 8 cycles, the data for the next tile is loaded into the upper 8 bits of this shift register.
    // Meanwhile, the pixel to render is fetched from one of the lower 8 bits.
    //
    private long tileData;
    //
    // 2 8-bit shift registers - These contain the palette attributes for the lower 8 pixels
    // of the 16-bit shift register. These registers are fed by a latch which contains the palette attribute
    // for the next tile. Every 8 cycles, the latch is loaded with the palette attribute for the next tile.
    //
    private int shiftRegister;
    // Name table byte
    private byte nameTableByte;
    // Attribute table byte
    private byte attrTableByte;
    //Pattern table tile low
    private byte tl;
    //Pattern table tile high (+8 bytes from pattern table tile low)
    private byte th;

    private int cycles;
    //Record current scan line index
    protected int scanline;
    //Record already trigger frame counter
    private long frameCounter;
    private int spriteCount;
    //Secondary OAM (holds 8 sprites for the current scanline)
    private final int[] sprites;
//    //8 counters - These contain the X positions for up to 8 sprites.
//    private final int[] spriteX;
//    //8 latches - These contain the attribute bytes for up to 8 sprites.
//    private final byte[] spriteAttr;

    public PPURender(PPU ppu) {
        this.ppu = ppu;
        this.cycles = 340;
        this.scanline = 240;
        this.frameCounter = 0;
        this.frame = new Frame();
        this.sprites = new int[8];
//        this.spriteX = new int[8];
//        this.spriteAttr = new byte[8];

        this.sysPalette = new int[DEF_SYS_PALETTE.length][];

        for (int i = 0; i < DEF_SYS_PALETTE.length; i++) {
            var src = DEF_SYS_PALETTE[i];
            var dst = new int[src.length];
            System.arraycopy(src, 0, dst, 0, dst.length);
            this.sysPalette[i] = dst;
        }
    }

    @Override
    public void tick() {
        this.cycles++;

        if (this.cycles == 341) {
            this.cycles = 0;
            this.scanline++;
        }

        this.render();

        if (this.scanline == 241) {
            //A frame render finish immediate output video
            this.ppu.context.videoOutput(this.frame);
            if (ppu.ctr.generateVBlankNMI()) {
                this.ppu.fireNMI();
                this.ppu.status.set(PStatus.V_BLANK_OCCUR);
            }
        }

        //
        // This is a dummy scanline, whose sole purpose is to fill the shift registers with the data for the first two tiles of the next scanline.
        // Although no pixels are rendered for this scanline,the PPU still makes the same memory accesses it would for a regular scanline.
        // This scanline varies in length, depending on whether an even or an odd frame is being rendered.
        // For odd frames, the cycle at the end of the scanline is skipped (this is done internally by jumping directly from (339,261) to (0,0),
        // replacing the idle tick at the beginning of the first visible scanline with the last tick of the last dummy nametable fetch).
        // For even frames, the last cycle occurs normally. This is done to compensate for some shortcomings with the way the PPU physically outputs its video signal,
        // the end result being a crisper image when the screen isn't scrolling. However, this behavior can be bypassed by keeping rendering disabled until after this scanline has passed,
        // which results in an image that looks more like a traditionally interlaced picture.
        // During pixels 280 through 304 of this scanline, the vertical scroll bits are reloaded if rendering is enabled.
        //
        if (this.scanline >= 262) {
            this.scanline = 0;
            this.frameCounter++;
            this.ppu.status.clear(PStatus.V_BLANK_OCCUR, PStatus.SPRITE_ZERO_HIT);
        }
    }

    public void render() {
        //
        // Pre-render scanline (-1 or 261)
        //
        // This is a dummy scanline, whose sole purpose is to fill the shift registers with the data for the first two tiles of the next scanline.
        // Although no pixels are rendered for this scanline, the PPU still makes the same memory accesses it would for a regular scanline.
        // This scanline varies in length, depending on whether an even or an odd frame is being rendered. For odd frames,
        // the cycle at the end of the scanline is skipped (this is done internally by jumping directly from (339,261) to (0,0),
        // replacing the idle tick at the beginning of the first visible scanline with the last tick of the last dummy nametable fetch).
        // For even frames, the last cycle occurs normally. This is done to compensate for some shortcomings with the way the PPU physically
        // outputs its video signal, the end result being a crisper image when the screen isn't scrolling. However, this behavior can be bypassed
        // by keeping rendering disabled until after this scanline has passed, which results in an image that looks more like a traditionally interlaced picture.
        // During pixels 280 through 304 of this scanline, the vertical scroll bits are reloaded if rendering is enabled.
        //
        //
        var preLine = (this.scanline == 261);

        //
        // Visible scanlines (0-239)
        //
        // These are the visible scanlines, which contain the graphics to be displayed on the screen.
        // This includes the rendering of both the background and the sprites. During these scanlines,
        // the PPU is busy fetching data, so the program should not access PPU memory during this time,
        // unless rendering is turned off.
        //
        var visibleLine = this.scanline < 240;
        //
        // Cycles 1-256
        //
        // The data for each tile is fetched during this phase. Each memory access takes 2 PPU cycles to complete,
        // and 4 must be performed per tile:
        //
        // 1.Nametable byte
        // 2.Attribute table byte
        // 3.Pattern table tile low
        // 4.Pattern table tile high (+8 bytes from pattern table tile low)
        //
        // The data fetched from these accesses is placed into internal latches, and then fed to the appropriate
        // shift registers when it's time to do so (every 8 cycles). Because the PPU can only fetch an attribute
        // byte every 8 cycles, each sequential string of 8 pixels is forced to have the same palette attribute.
        // Sprite zero hits act as if the image starts at cycle 2 (which is the same cycle that the shifters
        // shift for the first time), so the sprite zero flag will be raised at this point at the earliest. Actual pixel
        // output is delayed further due to internal render pipelining, and the first pixel is output during cycle 4.
        // The shifters are reloaded during ticks 9, 17, 25, ..., 257.
        //
        // Note: At the beginning of each scanline, the data for the first two tiles is already loaded into the shift
        // registers (and ready to be rendered), so the first tile that gets fetched is Tile 3.
        // While all of this is going on, sprite evaluation for the next scanline is taking place as a seperate process,
        // independent to what's happening here.
        //
        var visibleCycle = this.cycles > 0 && this.cycles <= 256;
        //
        // Cycles 321-336
        //
        // This is where the first two tiles for the next scanline are fetched, and loaded into the shift registers. Again,
        // each memory access takes 2 PPU cycles to complete, and 4 are performed for the two tiles:
        //
        // 1.Nametable byte
        // 2.Attribute table byte
        // 3.Pattern table tile low
        // 4.Pattern table tile high (+8 bytes from pattern table tile low)
        //
        var nextLine = this.cycles >= 321 && this.cycles <= 336;

        var mask = this.ppu.getMask();
        var sprite = mask.contain(MaskFlag.SHOW_SPRITES);
        var background = mask.contain(MaskFlag.SHOW_BACKGROUND);

        //Whether render is enable
        var enable = (background || sprite);

        if (enable) {
            if (visibleLine && visibleCycle) {
                this.renderPixel();
            }
            if (visibleCycle) {

                if (this.cycles == 257) {
                    this.spriteEvaluate();
                }

                var type = this.cycles % 8;
                var v = this.ppu.v;
                switch (type) {
                    case 1 -> this.readTileIdx(v);
                    case 3 -> this.readTileAttr(v);
                    case 5 -> this.readTileOffset(v, false);
                    case 7 -> this.readTileOffset(v, true);
                }
            }

            // The coarse X component of v needs to be incremented when the next tile is reached.
            if (preLine) {
                this.incX();
            }
            //
            // If rendering is enabled, the PPU copies all bits related to horizontal position from t to v:
            // v: ....A.. ...BCDEF <- t: ....A.. ...BCDEF
            if (this.cycles == 257) {
                this.ppu.v = uint16((this.ppu.v & 0xfbe0) | (this.ppu.t & 0x041f));
            }

            //
            // If rendering is enabled, at the end of vblank, shortly after the horizontal bits are copied from
            // t to v at dot 257, the PPU will repeatedly copy the vertical bits from t to v from dots 280 to 304,
            // completing the full initialization of v from t:
            //
            // v: GHIA.BC DEF..... <- t: GHIA.BC DEF.....
            //
            if (preLine && this.cycles >= 280 && this.cycles <= 304) {
                this.ppu.v = uint16((this.ppu.v & 0x841f) | (this.ppu.t & 0x7be0));
            }

            //
            // If rendering is enabled, fine Y is incremented at dot 256 of each scanline, overflowing to coarse Y,and
            // finally adjusted to wrap among the nametables vertically.Bits 12-14 are fine Y. Bits 5-9 are coarse Y.
            // Bit 11 selects the vertical nametable.
            //
            if (cycles == 256) {
                this.incY();
            }
        }
    }

    /**
     * <b>
     * The high bits of v are used for fine Y during rendering, and addressing nametable data only
     * requires 12 bits, with the high 2 CHR address lines fixed to the 0x2000 region.
     * <b/>
     * <pre>
     *      yyy NN YYYYY XXXXX
     *      ||| || ||||| +++++-- coarse X scroll
     *      ||| || +++++-------- coarse Y scroll
     *      ||| ++-------------- nametable select
     *      +++----------------- fine Y scroll
     * </pre>
     */
    private void readTileIdx(int v) {
        var idx = 0x2000 | (v & 0x0fff);
        this.nameTableByte = this.ppu.iRead(idx);
    }

    /**
     * <b>The low 12 bits of the attribute address are composed in the following way:</b>
     * <pre>
     *      NN 1111 YYY XXX
     *      || |||| ||| +++-- high 3 bits of coarse X (x/4)
     *      || |||| +++------ high 3 bits of coarse Y (y/4)
     *      || ++++---------- attribute offset (960 bytes)
     *      ++--------------- nametable select
     * </pre>
     */
    private void readTileAttr(int v) {
        var idx = 0x23c0 | (v & 0x0c00) | (v >> 4) & 0x38 | (v >> 2) & 0x07;
        this.attrTableByte = this.ppu.iRead(idx);
    }

    private void readTileOffset(int v, boolean high) {
        var y = (v >> 12) & 7;
        var table = ppu.ctr.bkNamePatternTable();
        var address = table + uint8(this.nameTableByte) * 16 + y;
        if (!high) {
            this.tl = this.ppu.iRead(address);
        } else {
            this.th = this.ppu.iRead(address + 8);
        }
    }

    /**
     * Bits 0-4 are incremented,with overflow toggling bit 10. This means that bits 0-4 count from 0 to 31 across
     * a single nametable,and bit 10 selects the current nametable horizontally.
     */
    private void incX() {
        var v = this.ppu.v;
        if ((v & 0x001f) == 31) {
            v &= ~0x001f;
            v ^= 0x0400;
        } else {
            v += 1;
        }
        this.ppu.v = uint16(v);
    }

    /**
     *
     * Row 29 is the last row of tiles in a nametable. To wrap to the next nametable when incrementing coarse Y
     * from 29, the vertical nametable is switched by toggling bit 11, and coarse Y wraps to row 0.
     * Coarse Y can be set out of bounds (> 29), which will cause the PPU to read the attribute data stored there
     * as tile data. If coarse Y is incremented from 31, it will wrap to 0, but the nametable will not switch.
     * For this reason, a write >= 240 to $2005 may appear as a "negative" scroll value, where 1 or 2 rows of
     * attribute data will appear before the nametable's tile data is reached. (Some games use this to move the
     * top of the nametable out of the <a href="https://www.nesdev.org/wiki/Overscan">Overscan</a> area.)
     *
     */
    private void incY() {
        var v = this.ppu.v;
        if ((v & 0x7000) != 0x7000) {
            v += 0x1000;
        } else {
            v &= ~0x7000;
            var y = (v & 0x03e0) >> 5;
            if (y == 29) {
                y = 0;
                v ^= 0x8000;
            } else if (y == 31) {
                y = 0;
            } else {
                y += 1;
            }
            v = (v & 0x03e0 | y << 5);
        }
        this.ppu.v = uint16(v);
    }

    private void renderPixel() {
        var x = this.cycles - 1;
        var y = this.scanline;
        var sprite = int8(0);
        var background = this.backgroundPixel();
        if (x < 8 && !this.ppu.mask.contain(MaskFlag.SHOW_BACKGROUND)) {
            background = 0;
        }
        if (x < 8 && this.ppu.mask.contain(MaskFlag.LEFTMOST_8PXL_SPRITE)) {
            sprite = 0;
        }

        var s = sprite % 4 != 0;
        var b = background % 4 != 0;
        var pixel = int8(0);
        if (!s && !b) {
            pixel = 0;
        } else if (!b && s) {
            pixel = int8(sprite | 0x10);
        } else if (b && !s) {
            pixel = background;
        } else {
            if (x == 255) {
                this.ppu.status.set(PStatus.SPRITE_ZERO_HIT);
            }
        }
        this.frame.update(x, y, pixel);
    }

    private byte backgroundPixel() {
        if (!this.ppu.mask.contain(MaskFlag.SHOW_BACKGROUND)) {
            return 0;
        }
        var b = this.tileData >> ((7 - this.ppu.x) * 4);
        return int8((int) (b & 0x0f));
    }

    /**
     * <a href="https://www.nesdev.org/wiki/PPU_sprite_evaluation">Sprite Evaluation</a>
     */
    private void spriteEvaluate() {
        var count = 0;
        var size = this.ppu.ctr.spriteSize();
        for (var i = 0; i < 64; i += 4) {
            //Y position of top of sprite
            var y = this.ppu.oam[i * 4];
            var df = this.scanline - uint8(y);
            //Whether sprite fall on current scanline
            if (df < 0 || df >= size) {
                continue;
            }
            if (count < 8) {
                this.sprites[count] = i;
            }
            count++;
        }
        if (count > 8) {
            count = 8;
            this.ppu.status.set(PStatus.SPRITE_OVERFLOW);
        }
        this.spriteCount = count;
    }
}
