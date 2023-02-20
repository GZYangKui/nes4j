package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

public class NRMapper extends Mapper {

    public NRMapper(Cartridge cartridge, NES context) {
        super(cartridge, context);
    }
}
