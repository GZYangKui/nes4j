package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.ByteReadWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;

@Slf4j
public class Bus implements ByteReadWriter {
    private final PPU ppu;
    private final JoyPad joyPad;
    private final MemoryMap memoryMap;
    private final BiConsumer<PPU, JoyPad> gameLoopCallback;

    public Bus(byte[] rpg, final PPU ppu, BiConsumer<PPU, JoyPad> gameLoopCallback) {
        this.ppu = ppu;
        this.joyPad = new JoyPad();
        this.memoryMap = new MemoryMap(rpg);
        this.gameLoopCallback = gameLoopCallback;
    }

    public Bus(byte[] rpg, final PPU ppu) {
        this(rpg, ppu, null);
    }


    /**
     * 获取当前rpg大小
     */
    public int rpgSize() {
        return this.memoryMap.getRpgSize();
    }

    @Override
    public byte read(int address) {
        if (address == 0x2002) {
            return this.ppu.readStatus();
        }
        if (address == 0x2004) {
            return this.ppu.readOam();
        }
        if (address == 0x2007) {
            return this.ppu.read(address);
        }
        //player1
        if (address == 0x4016) {
            return this.joyPad.read();
        }
        //player2
        if (address == 0x4017) {
            return 0;
        }
        //0x2008<=address<=0x4000 mirror 0x2000-0x2007
        if (address >= 0x2008 && address <= 0x3fff) {
            address = address & 0b00100000_00000111;
            return this.read(address);
        }
        return this.memoryMap.read(address);
    }

    public boolean pollPPUNMI() {
        return this.ppu.isNMI();
    }

    /**
     * 向内存中写入一字节数据
     */
    public void write(int address, byte b) {
        //https://www.nesdev.org/wiki/PPU_programmer_reference#Controller_($2000)_%3E_write
        if (address == 0x2000) {
            this.ppu.writeCtr(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Mask_($2001)_%3E_write
        if (address == 0x2001) {
            this.ppu.writeMask(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_address_($2003)_%3E_write
        if (address == 0x2003) {
            this.ppu.writeOamAddr(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Scroll_($2005)_%3E%3E_write_x2
        if (address == 0x2005) {

        }
        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_DMA_($4014)_%3E_write
        if (address == 0x4014) {
            var buffer = new byte[0x100];
            var msb = Byte.toUnsignedInt(b) << 8;
            for (int i = 0; i < 0x100; i++) {
                buffer[i] = this.read(msb + i);
            }
            this.ppu.writeOam(buffer);
        }
        //Write to ppu address
        if (address == 0x2006) {
            this.ppu.writeAddr(b);
        }
        //Write to ppu data
        if (address == 0x2007) {
            this.ppu.write(address, b);
        }
        //Write to standard controller
        if (address == 0x4016) {
            this.joyPad.write(b);
        }
        //0x2008<=address<=0x3fff mirror 0x2000-0x2007
        if (address >= 0x2008 && address <= 0x3fff) {
            address = address & 0b00100000_00000111;
            this.write(address, b);
            return;
        }
        this.memoryMap.write(address, b);
    }

    /**
     * 一个指令执行完毕触发当前函数
     */
    public void tick(int cycle) {
        var before = this.ppu.isNMI();
        //PPU时钟是CPU时钟的3倍
        this.ppu.tick(cycle * 3);
        var after = this.ppu.isNMI();
        if (!before && after && gameLoopCallback != null) {
            this.gameLoopCallback.accept(this.ppu, this.joyPad);
        }
    }
}
