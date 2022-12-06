package cn.navclub.nes4j.bin.ppu;

import cn.navclub.nes4j.bin.config.MaskFlag;
import cn.navclub.nes4j.bin.config.PStatus;
import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;

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
    //Secondary OAM (holds 8 sprites for the current scanline)
    private final byte[] oam;

    private final Frame frame;
    //
    //2 16-bit shift registers - These contain the pattern table data for two tiles.
    // Every 8 cycles, the data for the next tile is loaded into the upper 8 bits of this shift register.
    // Meanwhile, the pixel to render is fetched from one of the lower 8 bits.
    //
    private int backgroundTile;
    //
    //2 8-bit shift registers - These contain the palette attributes for the lower 8 pixels
    // of the 16-bit shift register. These registers are fed by a latch which contains the palette attribute
    // for the next tile. Every 8 cycles, the latch is loaded with the palette attribute for the next tile.
    //
    private int backgroundTileAttr;
    private int cycles;
    //Record current scan line index
    protected int scanline;
    //Record already trigger frame counter
    private long frameCounter;

    public PPURender(PPU ppu) {
        this.ppu = ppu;

        this.scanline = 0;
        this.frameCounter = 0;
        this.oam = new byte[8];
        this.frame = new Frame();

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
            if (ppu.control.generateVBlankNMI()) {
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
        // Cycle 0
        // This is an idle cycle. The value on the PPU address bus during this cycle appears to be the same CHR
        // address that is later used to fetch the low background tile byte starting at dot 5 (possibly calculated
        // during the two unused NT fetches at the end of the previous scanline).
        //
        var visibleCycle = this.cycles > 0 && this.cycles <= 256;
        //
        // Cycles 321-336
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
        }
    }

    private void renderPixel() {
        var x = this.cycles - 1;
        var y = this.scanline;
    }

}
