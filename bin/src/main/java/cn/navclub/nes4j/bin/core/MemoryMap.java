package cn.navclub.nes4j.bin.core;

import lombok.extern.slf4j.Slf4j;

/**
 * CPU内存映射
 */
@Slf4j
public class MemoryMap {
    //ram 2KB
    private static final int RAM_SIZE = 0x800;
    private static final int IO_REG_SIZE = 0x08;
    //sram
    private static final int SRAM_SIZE = 0x200;
    private static final int RAM_MIRRORS = 0x800;
    private static final int RAM_MIRRORS_END = 0x1FFF;
    private static final int IO_MIRRORS = 0x2008;
    private static final int IO_MIRRORS_END = 0x3FFF;
    private static final int RPG_ROM = 0x8000;
    private static final int RPG_ROM_END = 0xFFFF;


    private final byte[] ch;
    private final byte[] rpg;
    private final byte[] ram;
    private final byte[] sRam;
    private final byte[] ioRegs;


    public MemoryMap(final NESFile nesFile) {
        this.ch = nesFile.getCh();
        this.rpg = nesFile.getRgb();
        this.ram = new byte[RAM_SIZE];
        this.sRam = new byte[SRAM_SIZE];
        this.ioRegs = new byte[IO_REG_SIZE];
    }

    /**
     * 从映射地址中获取真实地址
     */
    private int map(int address) {
        if (RAM_MIRRORS <= address && address <= RAM_MIRRORS_END) {
            address &= 0b11111111111;
        }
        if (IO_MIRRORS <= address && address <= IO_MIRRORS_END) {
            address &= 0b10000000000111;
        }
        return address;
    }

    /**
     *
     * 从指定内存中读取内容
     *
     */
    public byte read(int address) {
        address = this.map(address);
        if (address < RAM_SIZE) {
            return this.ram[address];
        }
        if (address >= RPG_ROM) {
            return this.rpg[address - RPG_ROM];
        }
        log.warn("Unknown memory address:0x[{}] mapper.", Integer.toHexString(address));
        return 0;
    }

    public void write(int address, byte b) {
        if (address <= RAM_MIRRORS_END) {
            this.ram[map(address)] = b;
        }
        if (address >= IO_MIRRORS_END && address <= IO_MIRRORS_END) {

        }
        if (address >= RPG_ROM && address <= RPG_ROM_END) {
            log.warn("Attempt modify only read memory area.");
        }
    }
}
