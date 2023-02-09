package cn.navclub.nes4j.bin.ppu;

import cn.navclub.nes4j.bin.core.Component;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.ppu.register.PPUControl;
import cn.navclub.nes4j.bin.ppu.register.PPUMask;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.config.PStatus;
import cn.navclub.nes4j.bin.ppu.register.PPUStatus;
import lombok.Getter;
import lombok.Setter;

import static cn.navclub.nes4j.bin.util.BinUtil.*;
import static cn.navclub.nes4j.bin.util.MathUtil.u8add;
import static cn.navclub.nes4j.bin.util.MathUtil.u8sbc;


/**
 * <h1>PPU memory map</h1>
 * <p>
 * The PPU addresses a 16kB space, $0000-3FFF, completely separate from the CPU's address bus. It is either directly accessed by the PPU itself, or via the CPU with memory mapped registers at $2006 and $2007.
 * <p>
 * The NES has 2kB of RAM dedicated to the PPU, normally mapped to the nametable address space from $2000-2FFF, but this can be rerouted through custom cartridge wiring.
 * </p>
 * <table border="1">
 * <tr>
 *     <th>Address range</th>
 *     <th>Size</th>
 *     <th>Description</th>
 * </tr>
 * <tr>
 *     <td>$0000-$0FFF</td>
 *     <td>$1000</td>
 *     <td>Pattern table 0</td>
 * </tr>
 * <tr>
 *     <td>$1000-$1FFF</td>
 *     <td>$1000</td>
 *     <td>Pattern table 1</td>
 * </tr>
 * <tr>
 *     <td>$2000-$23FF</td>
 *     <td>$0400</td>
 *     <td>Name table 0</td>
 * </tr>
 * <tr>
 *     <td>$2000-$23FF</td>
 *     <td>$0400</td>
 *     <td>Name table 0</td>
 * </tr>
 * <tr>
 *     <td>$2400-$27FF</td>
 *     <td>$0400</td>
 *     <td>Name table 1</td>
 * </tr>
 * <tr>
 *     <td>$2800-$2BFF</td>
 *     <td>$0400</td>
 *     <td>Name table 2</td>
 * </tr>
 * <tr>
 *     <td>$2C00-$2FFF</td>
 *     <td>$0400</td>
 *     <td>Name table 3</td>
 * </tr>
 * <tr>
 *     <td>$3000-$3EFF</td>
 *     <td>$0F00</td>
 *     <td>Mirrors of $2000-$2EFF</td>
 * </tr>
 * <tr>
 *     <td>$3F00-$3F1F	</td>
 *     <td>$0020</td>
 *     <td>Palette RAM indexes</td>
 * </tr>
 * <tr>
 *     <td>$3F20-$3FFF	</td>
 *     <td>$00E0</td>
 *     <td>Mirrors of $3F00-$3F1F</td>
 * </tr>
 * </table>
 *
 * <p>More PPU detail please visit:<a href="https://www.nesdev.org/wiki/PPU_programmer_reference">PPU document</a></p>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class PPU implements Component {
    //The data necessary for render the screen
    @Getter
    protected final byte[] vram;
    @Getter
    protected final PPUMask mask;
    protected final PPUStatus status;
    private final Render render;
    @Getter
    protected final PPUControl ctr;
    @Getter
    protected final byte[] oam;
    /**
     * <h1>Color Palette</h1>
     * <p>
     * The NES has a colour palette containing 52 colours although there is actually room for 64.
     * However, not all of these can be displayed at a given time. The NES uses two palettes, each
     * with 16 entries, the image palette ($3F00-$3F0F) and the sprite palette ($3F10-$3F1F). The
     * image palette shows the colours currently available for background tiles. The sprite palette
     * shows the colours currently available for sprites. These palettes do not store the actual
     * colour values but rather the index of the colour in the system palette. Since only 64 unique
     * values are needed, bits 6 and 7 can be ignored.
     * </p>
     * <p>
     * The palette entry at $3F00 is the background colour and is used for transparency. Mirroring
     * is used so that every four bytes in the palettes is a copy of $3F00. Therefore $3F04, $3F08,
     * $3F0C, $3F10, $3F14, $3F18 and $3F1C are just copies of $3F00 and the total number of
     * 19
     * colours in each palette is 13, not 16 [5]. The total number of colours onscreen at any time is
     * therefore 25 out of 52. Both palettes are also mirrored to $3F20-$3FFF.
     * </p>
     */
    @Getter
    protected final byte[] palette;
    @Getter
    private int oamAddr;
    private byte byteBuf;
    @Getter
    @Setter
    private NameMirror mirrors;
    protected final NES context;
    @Getter
    //Current VRAM address (15 bits)
    protected int v;
    @Getter
    //Temporary VRAM address (15 bits); can also be thought of as the address of the top left onscreen tile.
    protected int t;
    //First or second write toggle (1 bit)
    protected byte w;
    //Fine X scroll (3 bits)
    protected byte x;

    public PPU(final NES context, byte[] ch, NameMirror mirrors) {
        this.oamAddr = 0;
        this.byteBuf = 0;
        this.context = context;
        this.mirrors = mirrors;
        this.oam = new byte[256];
        this.vram = new byte[2048];
        this.mask = new PPUMask();
        this.ctr = new PPUControl();
        this.status = new PPUStatus();
        this.palette = new byte[32];
        this.render = new Render(this);
    }

    public PPU(NES context, NameMirror mirrors) {
        this(context, new byte[2048], mirrors);
    }


    @Override
    public void reset() {
        this.t = 0;
        this.v = 0;
        this.w = 0;
        this.x = 0;
        this.render.reset();
        this.ctr.setBits(int8(0));
        this.mask.setBits(int8(0));
        this.status.setBits(int8(0));
    }

    @Override
    public void tick() {
        for (int i = 0; i < 3; i++) {
            this.render.tick();
        }
    }

    public void DMAWrite(byte[] arr) {
        for (byte b : arr) {
            this.oam[this.oamAddr] = b;
            this.oamAddr = u8add(this.oamAddr, 1);
        }
    }

    public void OAMWrite(byte b) {
        this.oam[this.oamAddr] = b;
        this.oamAddr = u8sbc(this.oamAddr, 1);
    }

    public byte readOam() {
        return this.oam[this.oamAddr];
    }

    public void OAMAddrWrite(byte b) {
        this.oamAddr = uint8(b);
    }

    public void AddrWrite(byte b) {
        if (this.w == 0) {
            this.w = 1;
            // t: .CDEFGH ........ <- d: ..CDEFGH
            //        <unused>     <- d: AB......
            // t: Z...... ........ <- 0 (bit Z is cleared)
            // w:                  <- 1
            this.t = uint16(this.t & 0x80ff | (uint8(b) & 0x3f) << 8);
        } else {
            //
            // t: ....... ABCDEFGH <- d: ABCDEFGH
            // v: <...all bits...> <- t: <...all bits...>
            // w:                  <- 0
            //
            this.w = 0;
            this.t = uint16(this.t & 0xff00 | uint8(b));
            this.v = this.t;
        }
    }

    @Override
    public byte read(int address) {
        var addr = this.v % 0x4000;

        //
        // After each write to $2007, the address is incremented by either 1 or 32 as dictated by
        // bit 2 of $2000. The first read from $2007 is invalid and the data will actually be buffered and
        // returned on the next read. This does not apply to colour palettes.
        //
        var temp = this.byteBuf;

        //Read pattern table
        if (addr < 0x2000) {
            this.byteBuf = this.context.getMapper().readCom(addr);
        }
        //Read name table
        else if (addr < 0x3f00) {
            this.byteBuf = this.vram[VRAMirror(addr)];
        }
        //Read palette table
        else {
            temp = this.palette[this.paletteMirror(addr)];
        }

        this.v += this.ctr.inc();

        return temp;
    }

    protected int iRead(int address) {
        final byte b;
        //Read chr-rom data
        if (address < 0x2000) {
            b = this.context.getMapper().readCom(address);
        }
        //Read name table data
        else if (address < 0x3f00) {
            b = this.vram[this.VRAMirror(address)];
        }
        //unknown ppu read memory
        else {
            b = 0;
        }

        return uint8(b);
    }

    @Override
    public void write(int address, byte b) {
        var addr = this.v % 0x4000;

        //Update pattern table
        if (addr < 0x2000) {
            this.context.getMapper().writeCom(addr, b);
        }
        //Update name table
        else if (addr < 0x3f00) {
            this.vram[this.VRAMirror(addr)] = b;
        }
        //Update palette value
        else {
            this.palette[this.paletteMirror(addr)] = b;
        }

        this.v += this.ctr.inc();
    }

    public byte readStatus() {
        var b = this.status.getBits();
        //w:      <- 0(Reset scroll and address register)
        this.w = 0;
        //Due to every read ppu status clear VBL so can't judge VBL whether end need use sprite zero
        this.status.clear(PStatus.V_BLANK_OCCUR);
        return b;
    }

    /**
     * <p>
     * The PPU uses the current VRAM address for both reading and writing PPU memory thru $2007,
     * and for fetching nametable data to draw the background. As it's drawing the background,
     * it updates the address to point to the nametable data currently being drawn.
     * Bits 10-11 hold the base address of the nametable minus $2000.
     * Bits 12-14 are the Y offset of a scanline within a tile.
     * </p>
     *
     * <h1>Horizontal</h1>
     * <pre>
     *     +---------+---------+
     *     +    A    +    A    +
     *     +---------+---------+
     *     +    B    +    B    +
     *     +---------+---------+
     * </pre>
     *
     * <h1>Vertical</h1>
     * <pre>
     *     +---------+---------+
     *     +    A    +    B    +
     *     +---------+---------+
     *     +    A    +    B    +
     *     +---------+---------+
     * </pre>
     * <h1>Single-Screen</h1>
     * <pre>
     *     +---------+---------+
     *     +    A    +    A    +
     *     +---------+---------+
     *     +    A    +    A    +
     *     +---------+---------+
     * </pre>
     * <h1>4-Screen</h1>
     * <pre>
     *     +---------+---------+
     *     +    A    +    B    +
     *     +---------+---------+
     *     +    C    +    D    +
     *     +---------+---------+
     * </pre>
     *
     * @param addr PPU address
     * @return Current nametable data address
     */
    private int VRAMirror(int addr) {
        addr = addr & 0x2fff;
        var idx = addr - 0x2000;
        var nameTable = idx / 0x400;
        if (mirrors == NameMirror.ONE_SCREEN || mirrors == NameMirror.ONE_SCREEN_UPPER)
            idx = addr % 0x27ff;
        else if (mirrors == NameMirror.ONE_SCREEN_LOWER)
            idx = addr % 0x23ff;
        else if ((mirrors == NameMirror.VERTICAL && (nameTable == 2 || nameTable == 3)))
            idx -= 0x800;
        else if (mirrors == NameMirror.HORIZONTAL && (nameTable == 1 || nameTable == 2))
            idx -= 0x400;
        else if (mirrors == NameMirror.HORIZONTAL && nameTable == 3)
            idx -= 0x800;
        return idx;
    }

    /**
     * <b>Palette memory view:</b>
     * <pre>
     * +****************+*******************+**********************+
     * +  Image palette +   Sprite palette  +       Mirrors        +
     * +****************+*******************+**********************+
     * + 0x3F00-0x3F10  +  0x3F10-0x3F20    +      $3F00-$3F1F     +
     * +****************+***************+***+**********************+
     * </pre>
     *
     * @param addr Palette address
     * @return Mirror after address
     */
    private int paletteMirror(int addr) {
        if (addr == 0x3f10 || addr == 0x3f14 || addr == 0x3f18 || addr == 0x3f1c) {
            addr = addr - 0x10;
        }
        return addr % 32;
    }

    /**
     * <p>
     * Another way of seeing the explanation above is that when you reach the end of a nametable,
     * you must switch to the next one, hence, changing the nametable address.
     * </p>
     * <p>
     * After power/reset, writes to this register are ignored for about 30,000 cycles.
     * </p>
     * <p>
     * If the PPU is currently in vertical blank, and the PPUSTATUS ($2002) vblank flag is still set (1), changing
     * the NMI flag in bit 7 of $2000 from 0 to 1 will immediately generate an NMI. This can result in graphical
     * errors (most likely a misplaced scroll) if the NMI routine is executed too late in the blanking period to
     * finish on time. To avoid this problem it is prudent to read $2002 immediately before writing $2000 to clear
     * the vblank flag.
     * </p>
     * <p>
     * For more explanation of sprite size, see: <a href="https://www.nesdev.org/wiki/Sprite_size">Sprite size</a>
     *
     * @param b Update byte value
     */
    public void writeCtr(byte b) {
        var bit = this.ctr.getBits();
        var firmNMI = ((bit & 0x80) == 0) && ((b & 0x80) == 0x80);
        if (firmNMI && this.status.contain(PStatus.V_BLANK_OCCUR)) {
            this.fireNMI();
        }
        this.ctr.update(b);
        //t: ...NN.. ........ <- d: ......NN
        this.t = uint16(this.t & 0xf3ff | (uint8(b) & 0x03) << 10);
    }

    public void ScrollWrite(byte b) {
        if (this.w == 0) {
            this.w = 1;
            //t: ........ ..ABCDE <- d: ABCDE...
            this.t = uint16(this.t & 0xffe0 | uint8(b) >> 3);
            //x:              FGH <- d: .....FGH
            this.x = int8(b & 0x07);
        } else {
            this.w = 0;
            //t: FGH..AB CDE..... <- d: ABCDEFGH
            this.t = uint16(this.t & 0x8fff | (((b & 0xff) & 0x07) << 12));
            this.t = uint16(this.t & 0xfc1f | (((b & 0xff) & 0xf8) << 2));
        }
    }

    public void MaskWrite(byte b) {
        this.mask.setBits(b);
    }

    public byte getStatus() {
        return this.status.getBits();
    }

    public long getScanline() {
        return this.render.scanline;
    }

    public int x() {
        return (this.v & 0x1f) | uint8(this.x) << 5;
    }

    public int y() {
        return (this.v >> 5) & 0x1f | (this.v >> 7 & 0x1f);
    }


    /**
     * Output one frame video sign.
     */
    protected void fireNMI() {
        this.status.set(PStatus.V_BLANK_OCCUR);
        if (this.ctr.generateVBlankNMI()) {
            this.context.interrupt(CPUInterrupt.NMI);
        }
    }
}
