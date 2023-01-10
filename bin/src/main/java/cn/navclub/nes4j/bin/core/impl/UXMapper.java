package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

public class UXMapper extends Mapper {

    public UXMapper(Cartridge cartridge, NES context) {
        super(cartridge, context);

        //Fix last bank to 0xc000
        System.arraycopy(
                this.cartridge.getRgbrom(),
                getLastBank(),
                this.rom,
                RPG_BANK_SIZE,
                RPG_BANK_SIZE
        );

        //Default copy all ch-rom data to cached.
        var ch = this.cartridge.getChrom();
        System.arraycopy(
                ch,
                0,
                this.com,
                0,
                Math.min(this.com.length, ch.length)
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
        var srcPos = bank * RPG_BANK_SIZE;
        System.arraycopy(this.cartridge.getRgbrom(), srcPos, this.rom, 0, RPG_BANK_SIZE);
    }
}
