package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.core.registers.MaskRegister;

/**
 *
 *
 *
 */
public class PPU {
    private final byte[] ch;
    private final byte[] vram;
    private final MaskRegister maskRegister;

    private int cycles;
    public PPU(byte[] ch) {
        this.ch = ch;
        this.vram = new byte[2048];
        this.maskRegister = new MaskRegister();
    }
}
