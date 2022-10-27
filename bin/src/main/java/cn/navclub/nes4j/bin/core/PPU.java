package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.ByteReadWriter;
import cn.navclub.nes4j.bin.core.impl.CTRegister;
import cn.navclub.nes4j.bin.core.impl.MKRegister;
import cn.navclub.nes4j.bin.enums.MaskFlag;
import cn.navclub.nes4j.bin.enums.PStatus;
import cn.navclub.nes4j.bin.util.MathUtil;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <a href="https://www.nesdev.org/wiki/PPU_programmer_reference">PPU document</a>
 */
public class PPU implements ByteReadWriter {

    @Getter
    private final byte[] ch;
    //The data necessary for render the screen
    @Getter
    private final byte[] vram;
    private final MKRegister mask;
    private final SRegister status;
    @Getter
    private final CTRegister control;
    private final PPUAddress addr;
    //Save sprite info total 64 each 4 byte.
    @Getter
    private final byte[] oam;
    @Getter
    //https://www.nesdev.org/wiki/PPU_palettes
    private final byte[] paletteTable;

    private int line;
    private int oamAddr;
    private byte readByteBuf;
    //判断是否发生NMI中断
    private final AtomicBoolean isNMI;

    private int cycles;
    //Mirrors
    @Getter
    private final int mirrors;
    @Getter
    private final PPUScroll scroll;

    public PPU(byte[] ch, int mirrors) {
        this.ch = ch;
        this.line = 0;
        this.oamAddr = 0;
        this.readByteBuf = 0;
        this.addr = new PPUAddress();
        this.oam = new byte[256];
        this.mirrors = mirrors;
        this.scroll = new PPUScroll();
        this.vram = new byte[2048];
        this.mask = new MKRegister();
        this.paletteTable = new byte[32];
        this.status = new SRegister();
        this.control = new CTRegister();
        this.isNMI = new AtomicBoolean(false);
    }

    /**
     * <a hrep="https://www.nesdev.org/wiki/PPU_rendering">PPU Render</a>
     */
    public void tick(int cycles) {
        this.cycles += cycles;
        if (this.cycles < 341) {
            return;
        }
        //每条扫描线条耗时341个PPU时钟约113.667个CPU时钟
        if (this.spriteHit(cycles)) {
            this.status.set(PStatus.SPRITE_ZERO_HIT);
        }
        this.line += 1;
        this.cycles -= 341;

        if (this.line == 241) {
            this.status.set(PStatus.V_BLANK_OCCUR);
            this.status.clear(PStatus.SPRITE_ZERO_HIT);
            if (this.control.generateVBlankNMI()) {
                this.isNMI.set(true);
            }
        }
        //一帧渲染完毕
        if (this.line >= 262) {
            this.line = 0;
            this.isNMI.set(false);
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
        var value = this.addr.update(b);
        if (value > 0x3fff) {
            this.addr.set(value & 0x3fff);
        }
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
        this.status.clear(PStatus.V_BLANK_OCCUR);
        this.addr.reset();
        this.scroll.reset();
        return b;
    }

    private int ramMirror(int addr) {
        var mirrorRam = addr & 0x2fff;
        var ramIndex = mirrorRam - 0x2000;
        var nameTable = ramIndex / 0x400;

        if ((mirrors == 1 && nameTable == 2) | (mirrors == 1 && nameTable == 3))
            return ramIndex - 0x800;
        else if (mirrors == 0 && (nameTable == 2 || nameTable == 1))
            return ramIndex - 0x400;
        else if (mirrors == 0 && nameTable == 3)
            return ramIndex - 0x800;
        return ramIndex;
    }

    private boolean spriteHit(int cycle) {
        var y = this.oam[0] & 0xff;
        var x = this.oam[3] & 0xff;
        return y == this.line && x <= cycle && this.mask.contain(MaskFlag.SHOW_SPRITES);
    }

    private void inc() {
        this.addr.inc(this.control.VRamIncrement());
    }

    public int getAddrVal() {
        return this.addr.get();
    }


    public void writeCtr(byte b) {
        var temp = this.control.generateVBlankNMI();
        this.control.update(b);
        if (!temp && this.control.generateVBlankNMI() && this.status.contain(PStatus.V_BLANK_OCCUR)) {
            this.isNMI.set(true);
        }
    }

    public void writeScroll(byte b) {
        this.scroll.write(b & 0xff);
    }

    public boolean isNMI() {
        return this.isNMI.getAndSet(false);
    }

    public void writeMask(byte b) {
        this.mask.setBits(b);
    }
}
