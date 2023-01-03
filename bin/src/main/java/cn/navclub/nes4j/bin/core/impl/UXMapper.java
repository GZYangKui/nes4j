package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

public class UXMapper extends Mapper {

    public UXMapper(Cartridge cartridge) {
        super(cartridge);
        System.arraycopy(
                this.cartridge.getRgbrom(),
                ((this.cartridge.getRgbSize() / RPG_UNIT) - 1) * RPG_UNIT,
                this.rom,
                RPG_UNIT,
                RPG_UNIT
        );
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
    public void writeRom(int address, byte b) {
        var bank = b & 0x0f;
        var srcPos = bank * RPG_UNIT;
        System.arraycopy(this.cartridge.getRgbrom(), srcPos, this.rom, 0, RPG_UNIT);
    }
}
