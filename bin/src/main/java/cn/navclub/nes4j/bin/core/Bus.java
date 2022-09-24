package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.util.ByteUtil;

public class Bus {
    private final MemoryMap memoryMap;

    public Bus(MemoryMap memoryMap) {
        this.memoryMap = memoryMap;
    }

    /**
     * 从内存中读取一个字节数据
     */
    public byte readByte(int address) {
        return this.memoryMap.read(address);
    }

    /**
     * 读无符号字节
     */
    public int readUSByte(int address) {
        var b = this.readByte(address);
        return Byte.toUnsignedInt(b);
    }

    /**
     * 想内存中写入一字节数据
     */
    public void writeByte(int address, byte b) {
        this.memoryMap.write(address, b);
    }


    /**
     * 以小端序形式读取数据
     */
    public int readInt(int address) {
        var lsb = Byte.toUnsignedInt(this.readByte(address));
        var msb = Byte.toUnsignedInt(this.readByte(address + 1));
        return msb << 8 | lsb;
    }

    /**
     *
     * 向指定地址写入整形数据
     *
     */
    public void writeInt(int address, int value) {
        this.writeByte(address, ByteUtil.overflow(value));
    }
}
