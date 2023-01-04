package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.config.NMapper;
import cn.navclub.nes4j.bin.io.Cartridge;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public abstract class Mapper {
    protected static final int RPG_UNIT = 16 * 1024;

    protected final byte[] rom;
    protected final byte[] com;
    protected final Cartridge cartridge;


    public Mapper(Cartridge cartridge) {
        this.cartridge = cartridge;
        this.com = new byte[8 * 1024];
        this.rom = new byte[RPG_UNIT * 2];
        var ch = this.cartridge.getChrom();

        //Default copy all ch-rom data to cached.
        System.arraycopy(
                ch,
                0,
                this.com,
                0,
                Math.min(this.com.length, ch.length)
        );
    }

    /**
     * Read from rpg-rom
     *
     * @param address Target address
     * @return rpg-rom data
     */
    public byte readRom(int address) {
        return this.rom[address];
    }

    /**
     * Write data to rpg-rom address
     *
     * @param b       Write target address value
     * @param address Target address
     */
    public void writeRom(int address, byte b) {

    }

    /**
     * Read from ch-rom
     *
     * @param address Target memory address
     * @return Target memory address value
     */
    public byte readCom(int address) {
        return this.com[address];
    }

    /**
     * Write data to ch-rom address
     *
     * @param address Target address
     * @param b       Write target address value
     */
    public void writeCom(int address, byte b) {
        this.com[address] = b;
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
        return ((this.cartridge.getRgbSize() / RPG_UNIT) - 1) * RPG_UNIT;
    }

    /**
     * NES instance reset call this function
     */
    public void reset() {

    }
}
