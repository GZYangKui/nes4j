package cn.navclub.nes4j.bin.config;

/**
 * <a href="https://www.nesdev.org/wiki/Category:INES_Mappers">INES Mappers</a>
 */
public enum NMapper {
    NROM(true),
    MMC1,
    /**
     * CPU $8000-$BFFF: 16 KB switchable PRG ROM bank
     * CPU $C000-$FFFF: 16 KB PRG ROM bank, fixed to the last bank
     * <p>
     * 7  bit  0
     * ---- ----
     * xxxx pPPP
     * ||||
     * ++++- Select 16 KB PRG ROM bank for CPU $8000-$BFFF
     * (UNROM uses bits 2-0; UOROM uses bits 3-0)
     */
    UX_ROM(true),
    MAPPER_003,
    MMC3,
    UNKNOWN;
    public final boolean impl;

    NMapper(boolean impl) {
        this.impl = impl;
    }

    NMapper() {
        this(false);
    }
}
