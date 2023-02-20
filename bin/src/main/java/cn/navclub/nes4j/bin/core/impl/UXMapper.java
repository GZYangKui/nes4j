package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

public class UXMapper extends Mapper {
    private int offset;

    public UXMapper(Cartridge cartridge, NES context) {
        super(cartridge, context);
    }

    /**
     * <pre>
     * CPU $8000-$BFFF: 16 KB switchable PRG ROM bank
     * CPU $C000-$FFFF: 16 KB PRG ROM bank, fixed to the last bank
     *
     * 7  bit  0
     * ---- ----
     * xxxx pPPP
     * ||||
     * ++++- Select 16 KB PRG ROM bank for CPU $8000-$BFFF
     * (UNROM uses bits 2-0; UOROM uses bits 3-0)
     * </pre>
     */
    @Override
    public void PRGWrite(int address, byte b) {
        this.offset = (b & 0x0f) * RPG_BANK_SIZE;
    }

    @Override
    public byte PRGRead(int address) {
        if (address >= 0x4000) {
            return this.cartridge.getRgbrom()[this.getLastBank() + (address % 0x4000)];
        }
        return this.cartridge.getRgbrom()[offset + address];
    }
}
