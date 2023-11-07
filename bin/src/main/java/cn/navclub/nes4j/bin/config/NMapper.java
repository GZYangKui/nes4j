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
