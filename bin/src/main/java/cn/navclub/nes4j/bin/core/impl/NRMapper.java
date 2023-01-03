package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

public class NRMapper extends Mapper {

    public NRMapper(Cartridge cartridge) {
        super(cartridge);
        System.arraycopy(cartridge.getChrom(), 0, this.com, 0, 8 * 1024);
        System.arraycopy(cartridge.getRgbrom(), 0, this.rom, 0, RPG_UNIT * 2);
    }

    @Override
    public void writeRom(int address, byte b) {
        throw new RuntimeException("Only read rom not allow modify.");
    }
}
