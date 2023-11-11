package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.config.NMapper;
import cn.navclub.nes4j.bin.io.Cartridge;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public abstract class Mapper {
    protected static final int CHR_BANK_SIZE = 8 * 1024;
    protected static final int PRG_BANK_SIZE = 16 * 1024;

    protected final NesConsole console;
    protected final Cartridge cartridge;

    public Mapper(Cartridge cartridge, NesConsole console) {
        this.console = console;
        this.cartridge = cartridge;
    }

    /**
     * Read from rpg-rom
     *
     * @param address Target address
     * @return rpg-rom data
     */
    public byte PRGRead(int address) {
        return this.cartridge.getRgbrom()[address];
    }

    /**
     * Write data to rpg-rom address
     *
     * @param b       Write target address value
     * @param address Target address
     */
    public void PRGWrite(int address, byte b) {

    }

    /**
     * Read from ch-rom
     *
     * @param address Target memory address
     * @return Target memory address value
     */
    public byte CHRead(int address) {
        return this.getChrom()[address];
    }

    /**
     * Write data to ch-rom address
     *
     * @param address Target address
     * @param b       Write target address value
     */
    public final void CHWrite(int address, byte b) {
        this.getChrom()[address] = b;
    }

    /**
     * Get ROM mapper type
     *
     * @return {@link NMapper}
     */
    public NMapper type() {
        return this.cartridge.getMapper();
    }

    protected int getLastBank() {
        return calMaxBankIdx() * PRG_BANK_SIZE;
    }

    public final int calMaxBankIdx() {
        return this.calMaxBankIdx(PRG_BANK_SIZE);
    }

    protected final int calMaxBankIdx(int unit) {
        return this.prgSize() / unit - 1;

    }

    /**
     * NES instance reset call this function
     */
    public void reset() {

    }

    public final int prgSize() {
        return this.cartridge.getRgbSize();
    }

    public final int chrSize() {
        return this.cartridge.getChSize();
    }

    public final byte[] getRgbrom() {
        return this.cartridge.getRgbrom();
    }

    public final byte[] getChrom() {
        return this.cartridge.getChrom();
    }

    /**
     * Some mapper implement need use extern {@link  Component} cycle driver
     */
    public void tick() {

    }
}
