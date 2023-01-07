package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

/**
 * <h2>
 * <a href="https://www.nesdev.org/wiki/INES_Mapper_003">INES Mapper 003(CNROM)</a>
 * </h2>
 * <b>Overview</b>
 *
 * <li>PRG ROM size: 16 KiB or 32 KiB</li>
 * <li>PRG ROM bank size: Not bankswitched</li>
 * <li>PRG RAM: None</li>
 * <li>CHR capacity: Up to 2048 KiB ROM</li>
 * <li>CHR bank size: 8 KiB</li>
 * <li>Nametable mirroring: Fixed vertical or horizontal mirroring</li>
 * <li>Subject to bus conflicts: Yes (CNROM), but not all compatible boards have bus conflicts.</li>
 *
 * <b>Register</b>
 * <pre>
 * 7  bit  0
 * ---- ----
 * cccc ccCC
 * |||| ||||
 * ++++-++++- Select 8 KB CHR ROM bank for PPU $0000-$1FFF
 *
 * CNROM only implements the lowest 2 bits, capping it at 32 KiB CHR. Other boards may implement 4 or more bits for larger CHR.
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class CNMapper extends Mapper {
    public CNMapper(Cartridge cartridge) {
        super(cartridge);
        System.arraycopy(cartridge.getRgbrom(), 0, this.rom, 0, cartridge.getRgbSize());
    }

    @Override
    public void writeRom(int address, byte b) {
        System.arraycopy(cartridge.getChrom(), (b & 0x03) * CHR_BANK_SIZE, this.com, 0, CHR_BANK_SIZE);
    }
}
