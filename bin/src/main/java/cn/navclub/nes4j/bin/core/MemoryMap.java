package cn.navclub.nes4j.bin.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * CPU内存映射
 */
@Slf4j
public class MemoryMap {
    @Getter
    private final int rpgSize;
    private final byte[] buffer;


    public MemoryMap(byte[] rpg) {
        this.rpgSize = rpg.length;
        this.buffer = new byte[0x10000];
        //复制rpg-rom到内存映射中
        System.arraycopy(rpg, 0, this.buffer, 0x8000, rpgSize);
    }

    /**
     * 从映射地址中获取真实地址
     */
    private int map(int address) {
        var address0 = address;
        if (0x800 <= address && address <= 0x1FFF) {
            address &= 0b11111111111;
        }

        //映射rpg内容
        if (address >= 0xC000 && rpgSize == 0x4000) {
            address = 0x8000 + (address - 0xC000);
        }
        if (address != address0) {
            log.debug("Memory address from {} mapper to {}.", address0, address);
        }

        return address;
    }

    /**
     * 从指定内存中读取内容
     */
    public byte read(int address) {
        address = this.map(address);

        if (address == 0x2000
                || address == 0x2001
                || address == 0x2003
                || address == 0x2005
                || address == 0x2006
                || address == 0x4014) {
//            log.warn("Attempt to read from write-only PPU address:0x{}", Integer.toHexString(address));
            return 0;
        }

        if (address >= 0x4000 && address < 0x4013) {
//            log.warn("Attempt to read from write-only APU address 0x{}", Integer.toHexString(address));
            return 0;
        }
//        if (address >= 0x4015) {
//            //todo Implement apu register
//            return 0;
//        }

        return this.buffer[address];
    }


    public void write(int address, byte b) {
        address = this.map(address);
        if (address >= 0x8000) {
            log.debug("Only memory:rpg-rom area can't modify.");
            return;
        }
        this.buffer[address] = b;
    }
}
