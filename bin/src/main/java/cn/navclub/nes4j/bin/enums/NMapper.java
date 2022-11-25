package cn.navclub.nes4j.bin.enums;

/**
 * <a href="https://www.nesdev.org/wiki/Category:INES_Mappers">INES Mappers</a>
 */
public enum NMapper {
    NROM,
    MMC1,
    /**
     * CPU $8000-$BFFF: 16 KB switchable PRG ROM bank
     * CPU $C000-$FFFF: 16 KB PRG ROM bank, fixed to the last bank
     *
     * 7  bit  0
     * ---- ----
     * xxxx pPPP
     *      ||||
     *      ++++- Select 16 KB PRG ROM bank for CPU $8000-$BFFF
     *           (UNROM uses bits 2-0; UOROM uses bits 3-0)
     */
    UX_ROM,
    MAPPER_003,
    MMC3,
    UNKNOWN
}
