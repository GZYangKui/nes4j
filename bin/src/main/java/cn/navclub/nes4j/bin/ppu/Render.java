package cn.navclub.nes4j.bin.ppu;

import cn.navclub.nes4j.bin.config.PStatus;
import cn.navclub.nes4j.bin.function.CycleDriver;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.ppu.register.PPUMask;
import lombok.Getter;

import java.util.Arrays;

import static cn.navclub.nes4j.bin.util.BinUtil.uint16;
import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * <a hrep="https://www.nesdev.org/wiki/PPU_rendering">PPU Render</a>
 * <p>
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
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class Render implements CycleDriver {
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
    private final PPUMask mask;
    // Name table byte
    private int tileIdx;
    // Attribute table byte
    private int tileAttr;
    //Pattern table tile low
    private int leftByte;
    //Pattern table tile high (+8 bytes from pattern table tile low)
    private int rightByte;

    protected int cycles;
    //Record current scan line index
    protected int scanline;

    //
    // Current scan line sprite pixel.
    //
    // piiiiii bbbbbbbb gggggggg rrrrrrrr
    // ||||||| |||||||| |||||||| ||||||||
    // ||||||| |||||||| |||||||| ||||||||
    // ||||||| |||||||| |||||||| ++++++++---------- red
    // ||||||| |||||||| ++++++++------------------- green
    // ||||||| ++++++++---------------------------- Blur
    // |++++++---------------------------------------------- Sprite index
    // +---------------------------------------------------- Priority (0: in front of background; 1: behind background)
    //
    private final int[] foreground;
    //sprite palette
    private final byte[] spritePalette;
    // Background pixel
    private final int[] background;
    //background palette
    private final byte[] backgroundPalette;
    //Background pixel shift
    private int shift;
    //Record product frame counter
    private long frames;
    //Whether odd frame
    private boolean odd;

    public Render(PPU ppu) {
        this.ppu = ppu;
        this.mask = ppu.mask;
        this.frame = new Frame();

        this.background = new int[16];
        this.foreground = new int[256];

        this.spritePalette = new byte[3];
        this.backgroundPalette = new byte[3];

        this.sysPalette = new int[DEF_SYS_PALETTE.length][];

        for (int i = 0; i < DEF_SYS_PALETTE.length; i++) {
            var src = DEF_SYS_PALETTE[i];
            var dst = new int[src.length];
            System.arraycopy(src, 0, dst, 0, dst.length);
            this.sysPalette[i] = dst;
        }
    }

    public void reset() {
        this.cycles = 0;
        this.frames = 0L;
        this.scanline = 240;
        this.frame.clear();
    }

    @Override
    public void tick() {
        //If [PPUMASK]] ($2001) with both BG and sprites disabled, rendering will be halted immediately.
        if (this.mask.enableRender()) {
            this.render();
        }
        //
        // The VBlank flag of the PPU is set at tick 1 (the second tick) of scanline 241,
        // where the VBlank NMI also occurs. The PPU makes no memory accesses during these scanlines,
        // so PPU memory can be freely accessed by the program.
        //
        if (this.scanline == 241 && this.cycles == 1) {
            this.frames++;
            this.odd = ((frames & 0x01) == 0x01);
            //Move to next scanline must reset shift
            this.shift = 0;
            this.ppu.fireNMI();
            //
            // A frame render finish if render enable immediate output video.
            // If render was disable output frame will product a white screen.
            //
            if (this.mask.enableRender()) {
                this.ppu.context.videoOutput(this.frame);
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


        var th = false;
        if (this.scanline == 261) {
            if (this.cycles == 1) {
                this.ppu.status.clear(PStatus.V_BLANK_OCCUR, PStatus.SPRITE_ZERO_HIT, PStatus.SPRITE_OVERFLOW);
            }
            //
            // This scanline varies in length, depending on whether an even or an odd frame is being rendered. For odd frames,
            // the cycle at the end of the scanline is skipped (this is done internally by jumping directly from (339,261) to (0,0),
            // replacing the idle tick at the beginning of the first visible scanline with the last tick of the last dummy nametable
            // fetch). For even frames, the last cycle occurs normally. This is done to compensate for some shortcomings with the way
            // the PPU physically outputs its video signal, the end result being a crisper image when the screen isn't scrolling.
            // However, this behavior can be bypassed by keeping rendering disabled until after this scanline has passed,
            // which results in an image that looks more like a traditionally interlaced picture.
            //
            // With rendering disabled (background and sprites disabled in PPUMASK ($2001)),
            // each PPU frame is 341*262=89342 PPU clocks long. There is no skipped clock every other frame.
            //
            th = this.mask.enableRender() && (this.odd && this.cycles == 339);
        }

        if ((++this.cycles) > 340 || th) {
            this.cycles = 0;
            this.scanline = (++this.scanline) % 262;
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
        var preLine = this.scanline == 261;

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
        // There are two behaviors:
        //
        // 1. When rendering is enabled and the current scanline is 261 or 0-239
        // 2. Any other time (vblank and anytime background and sprite rendering are both disabled)
        //
        // During 2, the address on the PPU bus is the low 14 bits of current value of v. If v changes,
        // the address on the bus changes. You do not do any automatic changes to v; the only time v changes
        // during 2 is on the 2nd write of $2006 (when the contents of t are copied to v) or when $2007 is
        // accessed (incrementing v by either 1 or 32 depending on the state of $2000).
        // You do not change v on dot 257. Leave v alone.
        //
        // During 1, the address on the bus is no longer v; instead, v is used to figure out what nametable,
        // CHR, and attributes addresses to fetch. v is changed automatically, including the vertical
        // increment on dot 256 and the hori(v) <- hori(t) change on dot 257.
        //
        // <a href="https://forums.nesdev.org/memberlist.php?mode=viewprofile&u=8835">Special thank nesdev user 'Fiskbit' helpe </a>
        //
        if (!(preLine || visibleLine)) {
            return;
        }

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
        var preFetchCycle = this.cycles >= 321 && this.cycles <= 336;

        var fetchCycle = preFetchCycle || visibleCycle;

        if (visibleLine && visibleCycle) {
            this.renderPixel();
        }

        if (fetchCycle) {
            var v = this.ppu.v;
            switch (this.cycles % 8) {
                case 0 -> this.tileMut();
                case 1 -> this.readTileIdx(v);
                case 3 -> this.readTileAttr(v);
                case 5 -> this.readTileByte(v, false);
                case 7 -> this.readTileByte(v, true);
            }
        }

        //
        // If rendering is enabled, fine Y is incremented at dot 256 of each scanline, overflowing to coarse Y,and
        // finally adjusted to wrap among the nametables vertically.Bits 12-14 are fine Y. Bits 5-9 are coarse Y.
        // Bit 11 selects the vertical nametable.
        //
        if (cycles == 256) {
            this.incY();
        }


        if (this.cycles == 257) {
            // Sprite evaluation does not happen on the pre-render scanline. Because evaluation applies to the next
            // line's sprite rendering, no sprites will be rendered on the first scanline, and this is why there is
            // a 1 line offset on a sprite's Y coordinate.
            if (!preLine) {
                this.spriteEval();
            } else {
                Arrays.fill(this.foreground, 0, this.foreground.length, -1);
            }
            //
            // At dot 257 of each scanline
            // If rendering is enabled, the PPU copies all bits related to horizontal position from t to v:
            // v: ....A.. ...BCDEF <- t: ....A.. ...BCDEF
            //
            this.ppu.v = uint16((this.ppu.v & 0xfbe0) | (this.ppu.t & 0x041f));
        }

        //
        // the PPU will repeatedly copy the vertical bits from t to v from dots 280 to 304,
        // completing the full initialization of v from t:
        //
        // v: GHIA.BC DEF..... <- t: GHIA.BC DEF.....
        //
        if (preLine && this.cycles >= 280 && this.cycles <= 304) {
            this.ppu.v = uint16((this.ppu.v & 0x841f) | (this.ppu.t & 0x7be0));
        }
    }

    private void tileMut() {
        //Copy upper 8 byte to lower 8 byte
        System.arraycopy(this.background, 8, this.background, 0, 8);

        var coarseX = (this.ppu.v & 0x1f);
        var coarseY = ((this.ppu.v >> 5) & 0x1f);


        var x = (coarseX % 4) / 2;
        var y = (coarseY % 4) / 2;

        /*
         *   ,---- Top left
         *   3 1 - Top right
         *   2 2 - Bottom right
         *   `---- Bottom left
         *   The byte has color set 2 at bottom right, 2 at bottom left, 1 at top right, and 3 at top left.
         *   Thus its attribute is calculated as follows:
         *   value = (bottomright << 6) | (bottomleft << 4) | (topright << 2) | (topleft << 0)
         *         = (2           << 6) | (2          << 4) | (1        << 2) | (3       << 0)
         *         = $80                | $20               | $04             | $03
         *         = $A7
         */
        var shift = x << 1 | y << 2;
        //Because first color was transparent so add 1
        var idx = 1 + ((this.tileAttr >> shift) & 0x03) * 4;

        this.backgroundPalette[0] = this.ppu.palette[idx];
        this.backgroundPalette[1] = this.ppu.palette[idx + 1];
        this.backgroundPalette[2] = this.ppu.palette[idx + 2];

        for (int i = 0; i < 8; i++) {
            var lower = (this.leftByte >> (7 - i)) & 0x01;
            var upper = (this.rightByte >> (7 - i)) & 0x01;
            var k = (lower | upper << 1);
            var color = switch (k) {
                case 1 -> this.rgbValue(this.backgroundPalette[0]);
                case 2 -> this.rgbValue(this.backgroundPalette[1]);
                case 3 -> this.rgbValue(this.backgroundPalette[2]);
                default -> this.rgbValue(ppu.palette[0]);
            };
            //Negative transparent otherwise opaque
            var value = (k == 0 ? 0x80000000 : 0) | color;
            this.background[i + 8] = value;
        }
        this.shift = 0;
        this.incX();
    }

    /**
     * <b>
     * The high bits of v are used for fine Y during rendering, and addressing nametable data only
     * requires 12 bits, with the high 2 CHR address lines fixed to the 0x2000 region.
     * <b/>
     * <pre>
     *      ... NN YYYYY XXXXX
     *          || ||||| +++++-- coarse X scroll
     *          || +++++-------- coarse Y scroll
     *          ++-------------- nametable select
     *
     * </pre>
     */
    private void readTileIdx(int v) {
        var address = 0x2000 | (v & 0x0fff);
        this.tileIdx = this.ppu.iRead(address);
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
        var address = 0x23c0 | (v & 0x0c00) | (v >> 4) & 0x38 | (v >> 2) & 0x07;
        this.tileAttr = this.ppu.iRead(address);
    }

    /**
     * From ppu address get fine y[yyy] after query tile in current position pixel.
     *
     * <b>PPU address:</b>
     *
     * <pre>
     *  yyy .. ..... .....
     *  |||
     *  +++----------------- fine Y scroll
     * </pre>
     *
     * <b>Explain example:</b>
     *
     * <pre>
     *  00000000 00010000
     *  10000000 00100000  <- y(Render current line 8 pixel)
     *  00000100 00010000
     *  00010000 00011000
     *  00000000 00000000
     *  00001000 00010000
     *  00000100 00111000
     *  00000010 00000000
     * </pre>
     */
    private void readTileByte(int v, boolean high) {
        var fineY = (v >> 12) & 0x07;
        var table = ppu.ctr.backgroundNameTable();
        var address = table + this.tileIdx * 16 + fineY;
        if (!high) {
            this.leftByte = this.ppu.iRead(address);
        } else {
            this.rightByte = this.ppu.iRead(address + 8);
        }
    }

    /**
     * The coarse X component of v needs to be incremented when the next tile is reached. Bits 0-4 are incremented,
     * with overflow toggling bit 10. This means that bits 0-4 count from 0 to 31 across a single nametable,
     * and bit 10 selects the current nametable horizontally.
     * <p/>
     * Implement pseudocode:
     * <pre>
     *    if ((v & 0x001F) == 31) // if coarse X == 31
     *     v &= ~0x001F          // coarse X = 0
     *     v ^= 0x0400           // switch horizontal nametable
     *    else
     *     v += 1                // increment coarse X
     * </pre>
     */
    private void incX() {
        var v = this.ppu.v;
        if ((v & 0x1f) == 31) {
            //coarse x=0
            v &= ~0x001f;
            //Switch horizontal name table
            // 00 ^ 01 = 01   (name table 0 -> 1)
            // 01 ^ 01 = 00   (name table 1 -> 0)
            // 10 ^ 01 = 11   (name table 2 -> 3)
            // 11 ^ 01 = 10   (name table 3 -> 2)
            v ^= 0x0400;
        } else {
            //Increase coarse x
            v++;
        }
        this.ppu.v = uint16(v);
    }

    /**
     * Row 29 is the last row of tiles in a nametable. To wrap to the next nametable when incrementing coarse Y
     * from 29, the vertical nametable is switched by toggling bit 11, and coarse Y wraps to row 0.
     * Coarse Y can be set out of bounds (> 29), which will cause the PPU to read the attribute data stored there
     * as tile data. If coarse Y is incremented from 31, it will wrap to 0, but the nametable will not switch.
     * For this reason, a write >= 240 to $2005 may appear as a "negative" scroll value, where 1 or 2 rows of
     * attribute data will appear before the nametable's tile data is reached. (Some games use this to move the
     * top of the nametable out of the <a href="https://www.nesdev.org/wiki/Overscan">Overscan</a> area.)
     */
    private void incY() {
        var v = this.ppu.v;
        // if fine Y < 7
        if ((v & 0x7000) != 0x7000) {
            // increment fine Y
            v += 0x1000;
        } else {
            // fine Y = 0
            v &= ~0x7000;
            // let y = coarse Y
            var y = (v & 0x03e0) >> 5;
            // Row 29 is the last row of tiles in a nametable.
            // To wrap to the next nametable when incrementing coarse Y from 29,
            // the vertical nametable is switched by toggling bit 11, and coarse Y wraps to row 0.
            if (y == 29) {
                // coarse Y = 0
                y = 0;
                // switch vertical nametable
                // 10 ^ 10 = 00     (name table 2 -> 0)
                // 00 ^ 10 = 10     (name table 0 -> 2)
                // 01 ^ 10 = 11     (name table 1 -> 3)
                // 11 ^ 10 = 01     (name table 3 -> 1)
                v ^= 0x0800;
            }
            // Coarse Y can be set out of bounds (> 29), which will cause the PPU to read the attribute
            // data stored there as tile data. If coarse Y is incremented from 31, it will wrap to 0,
            // but the nametable will not switch. For this reason, a write >= 240 to $2005 may appear
            // as a "negative" scroll value, where 1 or 2 rows of attribute data will appear before the
            // nametable's tile data is reached. (Some games use this to move the top of the nametable
            // out of the Overscan area.)
            else if (y == 31) {
                // coarse Y = 0, nametable not switched
                y = 0;
            } else {
                // increment coarse Y
                y++;
            }

            // put coarse Y back into v
            v = ((v & ~0x03e0) | y << 5);
        }
        this.ppu.v = uint16(v);
    }

    @SuppressWarnings("all")
    private void renderPixel() {
        var x = this.cycles - 1;
        var y = this.scanline;

        //Sprite color
        var forground = this.foreground[x];
        //Fetch background pixel,default is transparent color
        var pixel = rgbValue(this.ppu.palette[0]);
        var background = this.background[this.ppu.x + this.shift++];
        //Whether show sprite in position(x,y)
        var showSprite = (forground != -1 && this.mask.showSprite() && this.mask.showLeftMostSprite(x));
        //Whether show backage in position(x,y)
        var showBackground = (this.mask.showBackground() && this.mask.showLeftMostBackground(x));

        if (showBackground) {
            pixel = background;
        }

        //Check sprite pixel if cover background pixel
        if (showSprite) {
            var color = forground & 0xffffff;
            var index = (forground >> 24) & 0x3f;
            if (showBackground && pixel > 0 && index == 0 && x < 255) {
                this.ppu.status.set(PStatus.SPRITE_ZERO_HIT);
            }
            //If sprite priority or background is transparent
            if ((forground >> 30 & 0x01) == 0 || pixel < 0) {
                pixel = color;
            }
        }

        pixel |= (0xff << 24);

        this.frame.update(x, y, pixel);
    }

    /**
     * <a href="https://www.nesdev.org/wiki/PPU_sprite_evaluation">Sprite Evaluation</a>
     */
    private void spriteEval() {
        Arrays.fill(this.foreground, 0, this.foreground.length, -1);

        var count = 0;
        var size = this.ppu.ctr.spriteSize();
        for (var i = 0; i < 64; i++) {
            var offset = i * 4;
            var y = uint8(this.ppu.oam[offset]);
            var df = this.scanline - y;
            //Whether sprite fall on current scanline
            if (df < 0 || df >= size) {
                continue;
            }
            if (count < 8) {
                var x = uint8(this.ppu.oam[offset + 3]);
                var idx = uint8(this.ppu.oam[offset + 1]);
                var attr = uint8(this.ppu.oam[offset + 2]);
                //Indicates whether to flip the sprite horizontally.
                var hf = ((attr >> 6) & 0x01) == 1;
                //Indicates whether to flip the sprite vertically.
                var vf = ((attr >> 7) & 0x01) == 1;

                var bank = 0;
                var address = 0;

                //When sprite size is 8*16
                if (size == 0x10) {
                    bank = this.ppu.ctr.spritePattern16(idx);
                    if (vf) {
                        df = 15 - df;
                    }
                    idx &= 0xfe;
                    if (df > 7) {
                        idx++;
                        df -= 8;
                    }
                } else {
                    bank = this.ppu.ctr.spritePattern8();
                    if (vf) {
                        df = 7 - df;
                    }
                }

                address = bank + idx * 16 + df;

                var l = uint8(this.ppu.iRead(address));
                var r = uint8(this.ppu.iRead(address + 8));

                //Faster copy palette data
                System.arraycopy(ppu.palette, 0x11 + (attr & 0x03) * 4, this.spritePalette, 0, 3);

                for (int j = 0; j < 8; j++) {
                    var lower = (l >> (7 - j)) & 0x01;
                    var upper = (r >> (7 - j)) & 0x01;
                    var k = (lower | upper << 1);

                    var index = x + (hf ? (7 - j) : j);

                    if (k == 0 || index >= this.foreground.length) {
                        continue;
                    }

                    var b = rgbValue(this.spritePalette[k - 1]);

                    //Sprite index
                    b |= ((i & 0x3f) << 24);
                    //Prior
                    b |= ((attr & 0x20) << 25);

                    var value = this.foreground[index];
                    if (value == -1) {
                        this.foreground[index] = b;
                    }
                }
            }
            count++;
        }
        if (count > 8) {
            this.ppu.status.set(PStatus.SPRITE_OVERFLOW);
        }
    }

    private int rgbValue(int idx) {
        var arr = sysPalette[idx];
        return arr[0] << 16 | arr[1] << 8 | arr[2];
    }
}
