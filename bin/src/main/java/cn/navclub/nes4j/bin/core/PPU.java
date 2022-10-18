package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.ByteReadWriter;
import cn.navclub.nes4j.bin.core.impl.CTRegister;
import cn.navclub.nes4j.bin.core.impl.MKRegister;
import cn.navclub.nes4j.bin.enums.MaskFlag;
import cn.navclub.nes4j.bin.enums.PStatus;
import cn.navclub.nes4j.bin.util.MathUtil;
import lombok.Getter;

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
    private final byte[][] zeroSPixels;
    private final PPUAddress addr;
    //Save sprite info total 64 each 4 byte.
    private final byte[] oam;
    //https://www.nesdev.org/wiki/PPU_palettes
    private final byte[] paletteTable;

    private int line;
    private int oamAddr;
    private byte readByteBuf;

    private boolean nmiInterrupt;

    private int cycles;
    //Mirrors
    private final int mirrors;

    public PPU(byte[] ch, int mirrors) {
        this.ch = ch;
        this.line = 0;
        this.oamAddr = 0;
        this.readByteBuf = 0;
        this.addr = new PPUAddress();
        this.oam = new byte[256];
        this.mirrors = mirrors;
        this.vram = new byte[2048];
        this.mask = new MKRegister();
        this.paletteTable = new byte[32];
        this.zeroSPixels = new byte[0][0];
        this.status = new SRegister();
        this.control = new CTRegister();
    }

    /**
     * <a hrep="https://www.nesdev.org/wiki/PPU_rendering">PPU Render</a>
     */
    public void tick(int cycles) {
        this.cycles += cycles;
        //每条扫描线条耗时341个PPU时钟约113.667个CPU时钟
        if (this.cycles < 341) {
            return;
        }
        if (this.spriteHit(cycles)) {
            this.status.set(PStatus.SPRITE_ZERO_HIT);
        }
        this.line += 1;
        this.cycles %= 341;
        if (this.line < 241) {
            this.status.set(PStatus.V_BLANK_OCCUR, PStatus.SPRITE_ZERO_HIT);
            if (this.control.generateVBlankNMI()) {
            this.nmiInterrupt = true;
            }
        }
        if (this.line == 241) {
            this.status.set(PStatus.V_BLANK_OCCUR);
            this.status.clear(PStatus.SPRITE_ZERO_HIT);
            if (this.control.generateVBlankNMI()) {
            this.nmiInterrupt = true;
            }
        }
        //一帧渲染完毕
        if (this.line >= 262) {
            this.line = 0;
            this.nmiInterrupt = false;
            this.status.clear(PStatus.V_BLANK_OCCUR, PStatus.SPRITE_ZERO_HIT);
        }
    }

    public void writeOam(byte[] arr) {
        for (byte b : arr) {
            this.oam[this.oamAddr] = b;
            this.oamAddr = MathUtil.unsignedAdd(this.oamAddr, 1);
        }
    }

    public byte readOam() {
        return this.oam[this.oamAddr];
    }

    public void writeOamAddr(byte b) {
        this.oamAddr = (b & 0xff);
    }

    public void writeAddr(byte b) {
        var value = this.addr.set(b);
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
            if (addr >= 0x3f20) {
                addr = 0x3f00;
            }
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
            if (addr >= 0x3f20) {
                addr = 0x3f00;
            }
            this.paletteTable[addr - 0x3f00] = b;
        }
        this.inc();
    }

    public byte readStatus() {
        var b = this.status.getBits();
        this.status.update(PStatus.V_BLANK_OCCUR, false);
        this.addr.reset();
        return b;
    }

    private int ramMirror(int addr) {
        var mirrorRam = addr & 0b10111111111111;
        var ramIndex = mirrorRam - 0x2000;
        var nameTable = mirrorRam / 0x400;
        if ((mirrors == 1 && nameTable == 2) | (mirrors == 1 && nameTable == 3))
            return ramIndex - 0x800;
        else if (mirrors == 0 && (nameTable == 2 || nameTable == 1))
            return ramIndex - 0x400;
        else if (mirrors == 0 && nameTable == 3)
            return ramIndex - 0x800;
        else
            return nameTable;
    }

    private boolean spriteHit(int cycle) {
        var y = Byte.toUnsignedInt(this.oam[0]);
        var x = Byte.toUnsignedInt(this.oam[3]);
        return y + 5 == this.line && x <= cycle && this.mask.contain(MaskFlag.SHOW_SPRITES);
    }

    private void inc() {
        var value = this.addr.inc(this.control.VRamIncrement());
        if (value > 0x3fff) {
            this.addr.set(value & 0b11111111111111);
        }
    }

    public int getAddrVal() {
        return this.addr.get();
    }


    public void writeCtr(byte b) {
        var temp = this.control.generateVBlankNMI();
        this.control.update(b);
        if (!temp && this.control.generateVBlankNMI() && this.status.contain(PStatus.V_BLANK_OCCUR)) {
            this.nmiInterrupt = true;
        }
    }

    public boolean isNMI() {
        return this.nmiInterrupt;
    }

    public void writeMask(byte b) {
        this.mask.setBits(b);
    }
}
