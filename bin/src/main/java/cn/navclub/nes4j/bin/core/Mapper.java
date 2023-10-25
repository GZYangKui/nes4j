package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.config.NMapper;
import cn.navclub.nes4j.bin.io.Cartridge;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public abstract class Mapper {
    protected static final int CHR_BANK_SIZE = 8 * 1024;
    protected static final int RPG_BANK_SIZE = 16 * 1024;

    protected final byte[] chr;
    protected final NES context;
    protected final Cartridge cartridge;

    public Mapper(Cartridge cartridge, NES context) {
        this.context = context;
        this.cartridge = cartridge;
        this.chr = new byte[CHR_BANK_SIZE];
        System.arraycopy(cartridge.getChrom(), 0, this.chr, 0, Math.min(chrSize(), CHR_BANK_SIZE));
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
    public final byte CHRead(int address) {
        return this.chr[address];
    }

    /**
     * Write data to ch-rom address
     *
     * @param address Target address
     * @param b       Write target address value
     */
    public final void CHWrite(int address, byte b) {
        this.chr[address] = b;
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
        return calMaxBankIdx() * RPG_BANK_SIZE;
    }

    public final int calMaxBankIdx() {
        return this.prgSize() / RPG_BANK_SIZE - 1;
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
