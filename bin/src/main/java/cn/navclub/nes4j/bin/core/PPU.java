package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.core.impl.ControlRegister;
import cn.navclub.nes4j.bin.core.impl.MaskRegister;
import cn.navclub.nes4j.bin.enums.MaskFlag;
import cn.navclub.nes4j.bin.enums.PControl;
import cn.navclub.nes4j.bin.enums.PStatus;
import cn.navclub.nes4j.bin.screen.Frame;
import cn.navclub.nes4j.bin.util.ByteUtil;

/**
 *
 */
public class PPU {
    private static class Addr {
        private boolean msb;
        private final byte[] arr;

        public Addr() {
            this.msb = true;
            this.arr = new byte[2];
        }

        public void set(int data) {
            this.arr[0] = (byte) (data & 0xff);
            this.arr[1] = (byte) (data >> 8);
        }

        public int set(byte b) {
            this.arr[this.msb ? 1 : 0] = b;
            this.msb = !msb;
            return ByteUtil.toInt16(this.arr);
        }

        public int inc(int b) {
            var value = ByteUtil.toInt16(this.arr);
            value += b;
            this.arr[0] = (byte) (value & 0xff);
            this.arr[1] = (byte) (value << 8);
            return value;
        }

        public void msb() {
            this.msb = true;
        }

        public int get() {
            return ByteUtil.toInt16(arr);
        }

    }

    private final byte[] ch;
    private final byte[] vram;
    private final Frame frame;
    private final MaskRegister mask;
    private final SRegister status;
    private final ControlRegister control;
    private final byte[][] zeroSPixels;
    private final Addr addr;
    private final byte[] oam;
    private final byte[] palatteTable;

    private int line;
    private int oamAddr;
    private byte readByteBuf;


    private int cycles;

    public PPU(byte[] ch) {
        this.ch = ch;
        this.line = 0;
        this.oamAddr = 0;
        this.readByteBuf = 0;
        this.addr = new Addr();
        this.frame = new Frame();
        this.oam = new byte[256];
        this.vram = new byte[2048];
        this.mask = new MaskRegister();
        this.palatteTable = new byte[32];
        this.zeroSPixels = new byte[0][0];
        this.status = new SRegister();
        this.control = new ControlRegister();
    }

    public void tick(int cycles) {
        this.cycles += cycles;
        if (this.cycles < 341) {
            return;
        }
        if (this.spriteHit(cycles)) {
            this.status.set(PStatus.SPRITE_ZERO_HIT);
        }
        this.cycles -= 341;
        this.line += 1;
        if (this.line < 241) {

        }
        if (this.line == 241) {
            this.status.set(PStatus.V_BLANK_OCCUR);
            this.status.clear(PStatus.SPRITE_ZERO_HIT);
        }
        if (this.line >= 262) {
            this.line = 0;
            this.status.clear(PStatus.V_BLANK_OCCUR);
            this.status.clear(PStatus.SPRITE_ZERO_HIT);
        }
    }

    public void writeOam(byte[] arr) {
        for (byte b : arr) {
            this.oam[this.oamAddr] = b;
            ++this.oamAddr;
        }
    }

    public byte readOam() {
        return this.oam[this.oamAddr];
    }

    public void writeAddr(byte b) {
        var value = this.addr.set(b);
        if (value > 0x3fff) {
            this.addr.set(value & 0x3fff);
        }
    }

    public byte readByte() {
        var addr = this.addr.get();
        var temp = this.readByteBuf;
        this.incVRamAddr();
        if (addr >= 0 && addr <= 0x1fff) {
            this.readByteBuf = this.ch[addr];
        }
        if (addr >= 0x2000 && addr <= 0x2fff) {
            this.readByteBuf = this.vram[VRamMirror(addr)];
        }

        if (addr == 0x3f10 || addr == 0x3f14 || addr == 0x3f18 || addr == 0x3f1c) {
            var mirror = addr - 0x10;
            temp = this.palatteTable[mirror - 0x3f00];
        }

        if (addr >= 0x3f00 && addr <= 0x3fff) {
            temp = this.palatteTable[addr - 0x3f00];
        }

        return temp;
    }

    public void writeByte(byte b) {
        var addr = this.addr.get();
        if (addr >= 0 && addr <= 0x1fff) {
            //todo Only read memory area
        }
        if (addr >= 0x2000 && addr <= 0x2fff) {
            this.vram[this.VRamMirror(addr)] = b;
        }

        if (addr == 0x3f10 || addr == 0x3f14 || addr == 0x3f18 || addr == 0x3f1c) {
            addr = addr - 0x10;
            this.palatteTable[addr - 0x3f00] = b;
        }
        if (addr >= 0x3f00 && addr <= 0x3fff) {
            this.palatteTable[addr - 0x3f00] = b;
        }
        this.incVRamAddr();
    }

    public byte readStatus() {
        var b = this.status.getBits();
        this.status.update(PStatus.V_BLANK_OCCUR, false);
        this.addr.msb();
        return b;
    }

    private int VRamMirror(int addr) {
        var MVRam = addr & 0b10111111111111;
        var IRam = MVRam - 0x2000;
        var table = IRam / 0x400;

        return 0;
    }

    private boolean spriteHit(int cycle) {
        var y = Byte.toUnsignedInt(this.oam[0]);
        var x = Byte.toUnsignedInt(this.oam[3]);
        return y + 5 == this.line && x <= cycle && this.mask.contain(MaskFlag.SHOW_SPRITES);
    }

    private void incVRamAddr() {
        var value = this.addr.inc(this.control.VRamIncrement());
        if (value > 0x3fff) {
            this.addr.set(value & 0b11111111111111);
        }
    }


    public void writeControl(byte b) {
        this.control.update(b);
    }

    public void writeMask(byte b) {
        this.mask.setBits(b);
    }
}
