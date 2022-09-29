package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.core.registers.ControlRegister;
import cn.navclub.nes4j.bin.core.registers.MaskRegister;
import cn.navclub.nes4j.bin.core.registers.StatusRegister;
import cn.navclub.nes4j.bin.screen.Frame;

/**
 *
 */
public class PPU {
    private final byte[] ch;
    private final byte[] vram;
    private final Frame frame;
    private final MaskRegister mask;
    private final StatusRegister status;
    private final ControlRegister control;
    private final byte[][] zeroSPixels;

    private int line;
    private int oamAddr;
    private int readDataBuf;


    private int cycles;

    public PPU(byte[] ch) {
        this.ch = ch;
        this.line = 0;
        this.oamAddr = 0;
        this.readDataBuf = 0;
        this.frame = new Frame();
        this.vram = new byte[2048];
        this.mask = new MaskRegister();
        this.zeroSPixels = new byte[0][0];
        this.status = new StatusRegister();
        this.control = new ControlRegister();
    }

    public boolean tick(int cycles) {
        this.cycles += cycles;
        if (this.cycles < 341) {
            return false;
        }
        this.cycles -= 341;
        this.line += 1;
        if (this.line < 241) {

        }
        if (this.line == 241) {

        }
        if (this.line >= 262) {
            this.line = 0;
            this.status.spriteZeroHit(false);
            this.status.vbank(false);
            return true;
        }

        return false;
    }

    public byte readStatus() {
        return this.status.getBits();
    }

    public void writeControl(byte b) {
        this.control.update(b);
    }

    public void writeMask(byte b) {
        this.mask.update(b);
    }
}
