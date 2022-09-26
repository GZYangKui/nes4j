package cn.navclub.nes4j.bin.core;

import lombok.extern.slf4j.Slf4j;

/**
 * CPU内存映射
 */
@Slf4j
public class MemoryMap {

    private final byte[] buffer;


    public MemoryMap(byte[] rpg) {
        this.buffer = new byte[0x10000];
        //复制rpg-rom到内存映射中
        System.arraycopy(rpg, 0, this.buffer, 0x800, rpg.length);
    }

    /**
     * 从映射地址中获取真实地址
     */
    private int map(int address) {
        if (0x800 <= address && address <= 0x1FFF) {
            address &= 0b11111111111;
        }
        if (0x2008 <= address && address <= 0x3FFF) {
            address &= 0b10000000000111;
        }
        return address;
    }

    /**
     * 从指定内存中读取内容
     */
    public byte read(int address) {
        address = this.map(address);
        return this.buffer[address];
    }


    public void write(int address, byte b) {
        address = this.map(address);
        if (address >= 0x8000) {
            log.warn("Only memory:rpg-rom area can't modify.");
            return;
        }
        this.buffer[address] = b;
    }
}
