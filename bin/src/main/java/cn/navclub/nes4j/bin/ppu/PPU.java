package cn.navclub.nes4j.bin.ppu;

import cn.navclub.nes4j.bin.core.Component;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.config.StatusRegister;
import cn.navclub.nes4j.bin.ppu.register.PPUControl;
import cn.navclub.nes4j.bin.ppu.register.PPUMask;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.config.PStatus;
import cn.navclub.nes4j.bin.util.MathUtil;
import lombok.Getter;
import lombok.Setter;

import static cn.navclub.nes4j.bin.util.ByteUtil.uint16;
import static cn.navclub.nes4j.bin.util.ByteUtil.uint8;


/**
 * <a href="https://www.nesdev.org/wiki/PPU_programmer_reference">PPU document</a>
 */
public class PPU implements Component {
    @Getter
    private final byte[] ch;
    //The data necessary for render the screen
    @Getter
    protected final byte[] vram;
    @Getter
    private final PPUMask mask;
    protected final StatusRegister<PStatus> status;
    private final PPURender render;
    @Getter
    protected final PPUControl control;
    @Getter
    private final byte[] oam;
    @Getter
    //https://www.nesdev.org/wiki/PPU_palettes
    private final byte[] paletteTable;
    @Getter
    private int oamAddr;
    private byte byteBuf;
    @Getter
    @Setter
    private NameMirror mirrors;
    @Getter
    private final PPUScroll scroll;
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
    @Getter
    private long cycles;

    protected boolean nmi;

    public PPU(final NES context, byte[] ch, NameMirror mirrors) {
        this.oamAddr = 0;
        this.byteBuf = 0;
        this.context = context;
        this.mirrors = mirrors;
        this.oam = new byte[256];
        this.vram = new byte[2048];
        this.mask = new PPUMask();
        this.ch = new byte[8 * 1024];
        this.scroll = new PPUScroll();
        this.paletteTable = new byte[32];
        this.status = new StatusRegister();
        this.control = new PPUControl();
        this.render = new PPURender(this);

        System.arraycopy(ch, 0, this.ch, 0, Math.min(this.ch.length, ch.length));
    }

    public PPU(NES context, NameMirror mirrors) {
        this(context, new byte[2048], mirrors);
    }


    @Override
    public void tick() {
        this.cycles++;
        this.render.tick();
    }

    public void DMAWrite(byte[] arr) {
        for (byte b : arr) {
            this.oam[this.oamAddr] = b;
            this.oamAddr = MathUtil.u8add(this.oamAddr, 1);
        }
    }

    public void OAMWrite(byte b) {
        this.oam[this.oamAddr] = b;
        this.oamAddr = MathUtil.u8sbc(this.oamAddr, 1);
    }

    public byte readOam() {
        return this.oam[this.oamAddr];
    }

    public void OAMAddrWrite(byte b) {
        this.oamAddr = (b & 0xff);
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
        var addr = this.v;


        //
        // After each write to $2007, the address is incremented by either 1 or 32 as dictated by
        // bit 2 of $2000. The first read from $2007 is invalid and the data will actually be buffered and
        // returned on the next read. This does not apply to colour palettes.
        //
        var temp = this.byteBuf;

        if (addr >= 0 && addr <= 0x1fff) {
            this.byteBuf = this.ch[addr];
        }

        if (addr >= 0x2000 && addr <= 0x2fff) {
            this.byteBuf = this.vram[ramMirror(addr)];
        }

        if (addr == 0x3f10 || addr == 0x3f14 || addr == 0x3f18 || addr == 0x3f1c) {
            var mirror = addr - 0x10;
            temp = this.paletteTable[mirror - 0x3f00];
        }

        //读取调色板数据
        if (addr >= 0x3f00 && addr <= 0x3fff) {
            temp = this.paletteTable[addr - 0x3f00];
        }

        this.v += this.control.VRamIncrement();

        return temp;
    }

    @Override
    public void write(int address, byte b) {
        var addr = this.v;

        if (addr >= 0x00 && addr <= 0x1fff) {
            this.ch[addr] = b;
        }

        if (addr >= 0x2000 && addr <= 0x2fff) {
            this.vram[this.ramMirror(addr)] = b;
        }

        if (addr == 0x3f10 || addr == 0x3f14 || addr == 0x3f18 || addr == 0x3f1c) {
            addr = addr - 0x10;
            this.paletteTable[addr - 0x3f00] = b;
        }

        //更新调色板数据
        if (addr >= 0x3f00 && addr <= 0x3fff) {
            this.paletteTable[addr - 0x3f00] = b;
        }

        this.v += this.control.VRamIncrement();
    }

    public byte readStatus() {
        var b = this.status.getBits();
        //w:      <- 0(Reset scroll and address register)
        this.w = 0;
        //Due to every read ppu status clear VBL so can't judge VBL whether end need use sprite zero
        this.status.clear(PStatus.V_BLANK_OCCUR);
        return b;
    }

    private int ramMirror(int addr) {
        var mirrorRam = addr & 0x2fff;
        var ramIndex = mirrorRam - 0x2000;
        var nameTable = ramIndex / 0x400;

        if ((mirrors == NameMirror.VERTICAL && (nameTable == 2 || nameTable == 3)))
            ramIndex -= 0x800;
        else if (mirrors == NameMirror.HORIZONTAL && (nameTable == 2 || nameTable == 1))
            ramIndex -= 0x400;
        else if (mirrors == NameMirror.HORIZONTAL && nameTable == 3)
            ramIndex -= 0x800;
        return ramIndex;
    }

    public void writeCtr(byte b) {
        //After power/reset, writes to this register are ignored for about 30,000 cycles.
        if (this.cycles < 30000) {
            return;
        }

        var temp = this.control.generateVBlankNMI();
        this.control.update(b);
        if (!temp && this.control.generateVBlankNMI() && this.status.contain(PStatus.V_BLANK_OCCUR)) {
            this.fireNMI();
        }

        //t: ...GH.. ........ <- d: ......GH
        this.t = uint16(this.t & 0xf3ff | (uint8(b) & 0x03) << 10);
    }

    public void ScrollWrite(byte b) {
        this.scroll.write(uint8(b));
        if (this.w == 0) {
            this.w = 1;
            //t: ........ ..ABCDE <- d: ABCDE...
            this.t = uint16(this.t & 0xffe0 | uint8(b) >> 3);
            //x:              FGH <- d: .....FGH
            this.x = (byte) (b & 0x07);
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
        return this.v & 0x1f;
    }

    public int y() {
        return (this.v >> 5) & 0x1f;
    }

    /**
     *
     * Output one frame video sign.
     *
     */
    protected void fireNMI() {
        if (this.nmi) {
            return;
        }
        this.nmi = true;
        this.context.interrupt(CPUInterrupt.NMI);
    }
}
