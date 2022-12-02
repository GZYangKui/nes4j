package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.Component;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.impl.CTRegister;
import cn.navclub.nes4j.bin.core.impl.MKRegister;
import cn.navclub.nes4j.bin.enums.CPUInterrupt;
import cn.navclub.nes4j.bin.enums.MaskFlag;
import cn.navclub.nes4j.bin.enums.NameMirror;
import cn.navclub.nes4j.bin.enums.PStatus;
import cn.navclub.nes4j.bin.screen.Render;
import cn.navclub.nes4j.bin.util.MathUtil;
import lombok.Getter;
import lombok.Setter;


/**
 * <a href="https://www.nesdev.org/wiki/PPU_programmer_reference">PPU document</a>
 */
public class PPU implements Component {
    @Getter
    private final byte[] ch;
    //The data necessary for render the screen
    @Getter
    private final byte[] vram;
    @Getter
    private final MKRegister mask;
    @Getter
    private final SRegister status;
    @Getter
    private final CTRegister control;
    @Getter
    private final PPUAddress addr;
    //Save sprite info total 64 each 4 byte.
    @Getter
    private final byte[] oam;
    @Getter
    //https://www.nesdev.org/wiki/PPU_palettes
    private final byte[] paletteTable;
    @Getter
    private int oamAddr;
    @Getter
    private int scanLine;
    private byte readByteBuf;
    private long cycles;
    //Mirrors
    @Getter
    @Setter
    private NameMirror mirrors;
    @Getter
    private final PPUScroll scroll;
    private final NES context;
    private boolean nmi;

    public PPU(final NES context, byte[] ch, NameMirror mirrors) {
        this.oamAddr = 0;
        this.scanLine = 0;
        this.readByteBuf = 0;
        this.context = context;
        this.mirrors = mirrors;
        this.oam = new byte[256];
        this.vram = new byte[2048];
        this.addr = new PPUAddress();
        this.mask = new MKRegister();
        this.ch = new byte[8 * 1024];
        this.scroll = new PPUScroll();
        this.paletteTable = new byte[32];
        this.status = new SRegister();
        this.control = new CTRegister();

        System.arraycopy(ch, 0, this.ch, 0, Math.min(this.ch.length, ch.length));
    }

    public PPU(NES context, NameMirror mirrors) {
        this(context, new byte[2048], mirrors);
    }

    /**
     * <a hrep="https://www.nesdev.org/wiki/PPU_rendering">PPU Render</a>
     */
    @Override
    public void tick() {
        this.cycles += 3;

        //判断是否发生水平消隐(从当前行右侧回到下一行左侧)
        if (this.cycles < 341) {
            return;
        }

        //每条扫描线条耗时341个PPU时钟约113.667个CPU时钟
        if (this.spriteHit()) {
            this.status.set(PStatus.SPRITE_ZERO_HIT);
        }

        this.scanLine += 1;
        this.cycles %= 341;

        if (this.scanLine == 241) {
            this.status.set(PStatus.V_BLANK_OCCUR);
            this.status.clear(PStatus.SPRITE_ZERO_HIT);
            if (this.control.generateVBlankNMI()) {
                this.fireNMI();
            }
        }

        //一帧渲染完毕 垂直消隐(从右下角回到左上角)
        if (this.scanLine >= 262) {
            this.nmi = false;
            this.scanLine = 0;
            //Sprite 0 notify cpu VBL already end.
            this.status.clear(PStatus.V_BLANK_OCCUR, PStatus.SPRITE_ZERO_HIT);
        }
    }

    public void writeOam(byte[] arr) {
        for (byte b : arr) {
            this.oam[this.oamAddr] = b;
            this.oamAddr = MathUtil.unsignedAdd(this.oamAddr, 1);
        }
    }

    public void writeOamByte(byte b) {
        this.oam[this.oamAddr] = b;
        this.oamAddr = MathUtil.unsignedAdd(this.oamAddr, 1);
    }

    public byte readOam() {
        return this.oam[this.oamAddr];
    }

    public void writeOamAddr(byte b) {
        this.oamAddr = (b & 0xff);
    }

    public void writeAddr(byte b) {
        this.addr.update(b);
    }

    @Override
    public byte read(int address) {
        var addr = this.addr.get();
        this.inc();
        var temp = this.readByteBuf;

        if (addr >= 0 && addr <= 0x1fff) {
            this.readByteBuf = this.ch[addr];
        }

        if (addr >= 0x2000 && addr <= 0x2fff) {
            this.readByteBuf = this.vram[ramMirror(addr)];
        }

        if (addr == 0x3f10 || addr == 0x3f14 || addr == 0x3f18 || addr == 0x3f1c) {
            var mirror = addr - 0x10;
            temp = this.paletteTable[mirror - 0x3f00];
        }

        //读取调色板数据
        if (addr >= 0x3f00 && addr <= 0x3fff) {
            temp = this.paletteTable[addr - 0x3f00];
        }


        return temp;
    }

    @Override
    public void write(int address, byte b) {
        var addr = this.addr.get();

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

        this.inc();
    }

    public byte readStatus() {
        var b = this.status.getBits();
        //Due to every read ppu status clear VBL so can't judge VBL whether end need use sprite zero
        this.status.clear(PStatus.V_BLANK_OCCUR);
        this.addr.reset();
        this.scroll.reset();
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

    //
    //
    // Sprite zero hits
    // Sprites are conventionally numbered 0 to 63. Sprite 0 is the sprite controlled by OAM addresses $00-$03, sprite 1 is controlled by $04-$07, ..., and sprite 63 is controlled by $FC-$FF.
    //
    // While the PPU is drawing the picture, when an opaque pixel of sprite 0 overlaps an opaque pixel of the background, this is a sprite zero hit. The PPU detects this condition and sets bit 6 of PPUSTATUS ($2002) to 1 starting at this pixel, letting the CPU know how far along the PPU is in drawing the picture.
    //
    // Sprite 0 hit does not happen:
    //
    // If background or sprite rendering is disabled in PPUMASK ($2001)
    // At x=0 to x=7 if the left-side clipping window is enabled (if bit 2 or bit 1 of PPUMASK is 0).
    // At x=255, for an obscure reason related to the pixel pipeline.
    // At any pixel where the background or sprite pixel is transparent (2-bit color index from the CHR pattern is %00).
    // If sprite 0 hit has already occurred this frame. Bit 6 of PPUSTATUS ($2002) is cleared to 0 at dot 1 of the pre-render line. This means only the first sprite 0 hit in a frame can be detected.
    // Sprite 0 hit happens regardless of the following:
    //
    // Sprite priority. Sprite 0 can still hit the background from behind.
    // The pixel colors. Only the CHR pattern bits are relevant, not the actual rendered colors, and any CHR color index except %00 is considered opaque.
    // The palette. The contents of the palette are irrelevant to sprite 0 hits. For example: a black ($0F) sprite pixel can hit a black ($0F) background as long as neither is the transparent color index %00.
    // The PAL PPU blanking on the left and right edges at x=0, x=1, and x=254 (see Overscan).
    //
    //
    private boolean spriteHit() {
        var x = this.oam[3] & 0xff;
        var y = this.oam[0] & 0xff;
        //
        // Set when a nonzero pixel of sprite 0 overlaps a nonzero background pixel;
        // Sprite 0 hit does not trigger in any area where the background or sprites are hidden.
        //
        var show = this.mask.contain(MaskFlag.SHOW_SPRITES) && this.mask.contain(MaskFlag.SHOW_BACKGROUND);
        return show && y + 5 == this.scanLine && x <= this.cycles;
    }

    private void inc() {
        this.addr.inc(this.control.VRamIncrement());
    }

    public void writeCtr(byte b) {
        var temp = this.control.generateVBlankNMI();
        this.control.update(b);
        if (!temp && this.control.generateVBlankNMI() && this.status.contain(PStatus.V_BLANK_OCCUR)) {
            this.fireNMI();
        }
    }

    public void writeScroll(byte b) {
        this.scroll.write(Byte.toUnsignedInt(b));
    }

    public void writeMask(byte b) {
        this.mask.setBits(b);
    }

    /**
     *
     * Output one frame video sign.
     *
     */
    private void fireNMI() {
        if (this.nmi) {
            return;
        }
        this.nmi = true;
        this.context.interrupt(CPUInterrupt.NMI);
    }
}
