package cn.navclub.nes4j.bin.config;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.core.impl.*;
import cn.navclub.nes4j.bin.io.Cartridge;


/**
 * <a href="https://www.nesdev.org/wiki/Category:INES_Mappers">INES Mappers</a>
 */
public enum NMapper {
    NROM(NRMapper.class),
    MMC1(MMC1Mapper.class),
    UX_ROM(UXMapper.class),
    CN_ROM(CNMapper.class),
    MMC3(MMC3Mapper.class),
    NOT_IMPL_5,
    NOT_IMPL_6,
    NOT_IMPL_7,
    NOT_IMPL_8,
    NOT_IMPL_9,
    NOT_IMPL_10,
    NOT_IMPL_11,
    NOT_IMPL_12,
    NOT_IMPL_13,
    NOT_IMPL_14,
    NOT_IMPL_15,
    NOT_IMPL_16,
    NOT_IMPL_17,
    NOT_IMPL_18,
    NOT_IMPL_19,
    NOT_IMPL_20,
    NOT_IMPL_21,
    NOT_IMPL_22,
    KONAMI_VRC24(KonamiVRC24.class),
    UNKNOWN;

    private final Class<? extends Mapper> provider;

    NMapper(Class<? extends Mapper> provider) {
        this.provider = provider;
    }

    NMapper() {
        this(null);
    }

    @SuppressWarnings("all")
    public <T> T newProvider(Cartridge cartridge, NesConsole console) {
        try {
            return (T) this
                    .provider
                    .getDeclaredConstructor(Cartridge.class, NesConsole.class)
                    .newInstance(cartridge, console);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isImpl() {
        return this.provider != null;
    }
}
