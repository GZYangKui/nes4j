package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Bus {
    private final PPU ppu;
    private final MemoryMap memoryMap;

    private int cycle;

    public Bus(NESFile file) {
        this.ppu = new PPU(file.getCh());
        this.memoryMap = new MemoryMap(file.getRgb());
    }

    /**
     * 从内存中读取一个字节数据
     */
    public byte readByte(int address) {
        if (address == 0x2002) {
            return this.ppu.readStatus();
        }
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
     * 向内存中写入一字节数据
     */
    public void writeByte(int address, byte b) {
        if (address == 0x2000) {
            this.ppu.writeControl(b);
        }

        if (address == 0x2001) {
            this.ppu.writeMask(b);
        }

        if (address == 0x2002) {
            log.warn("Attempt to modify ppu status register.");
            return;
        }
        if (address == 0x4104) {
            var buffer = new byte[0x100];
            var msb = b << 8;
            for (int i = 0; i < 0x100; i++) {
                buffer[i] = this.readByte(msb + i);
            }
            //todo ppu oam
        }
        this.memoryMap.write(address, b);
    }

    /**
     * 写入无符号字节
     */
    public void writeUSByte(int address, int data) {
        this.memoryMap.write(address, ByteUtil.overflow(data));
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
     * 向指定地址写入整形数据
     */
    public void writeInt(int address, int value) {
        this.writeByte(address, ByteUtil.overflow(value));
    }

    /**
     * 一个指令执行完毕触发当前函数
     */
    public void tick(int cycle) {
        this.cycle += cycle;
        //触发PPU模块

    }
}
