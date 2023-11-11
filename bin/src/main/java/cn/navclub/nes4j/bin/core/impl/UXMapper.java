package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

public class UXMapper extends Mapper {
    private int offset;
    private final int[] PRGBank;

    public UXMapper(Cartridge cartridge, NesConsole console) {
        super(cartridge, console);
        this.PRGBank = new int[2];
        this.PRGBank[1] = this.calMaxBankIdx();
    }

    /**
     * <pre>
     * CPU $8000-$BFFF: 16 KB switchable PRG ROM bank
     * CPU $C000-$FFFF: 16 KB PRG ROM bank, fixed to the last bank
     *
     * 7  bit  0
     * ---- ----
     * xxxx pPPP
     *      ||||
     *      ++++- Select 16 KB PRG ROM bank for CPU $8000-$BFFF
     * (UNROM uses bits 2-0; UOROM uses bits 3-0)
     * </pre>
     */
    @Override
    public void PRGWrite(int address, byte b) {
        this.PRGBank[0] = b & this.PRGBank[1];
    }

    @Override
    public byte PRGRead(int address) {
        var idx = address / 0x4000;
        var offset = address % 0x4000;
        return super.PRGRead((this.PRGBank[idx] * PRG_BANK_SIZE) + offset);
    }
}
