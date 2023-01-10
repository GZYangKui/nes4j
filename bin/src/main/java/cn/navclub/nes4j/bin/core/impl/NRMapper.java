package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

public class NRMapper extends Mapper {

    public NRMapper(Cartridge cartridge, NES context) {
        super(cartridge, context);
        System.arraycopy(cartridge.getChrom(), 0, this.com, 0, Math.min(cartridge.getChSize(), 8 * 1024));
        System.arraycopy(cartridge.getRgbrom(), 0, this.rom, 0, Math.min(cartridge.getRgbSize(), RPG_BANK_SIZE * 2));
    }
}
