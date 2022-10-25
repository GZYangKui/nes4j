package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.ByteReadWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

@Slf4j
public class Bus implements ByteReadWriter {
    private final PPU ppu;
    @Getter
    private final int rpgSize;
    //Cpu memory mapper
    private final byte[] buffer;
    private final JoyPad joyPad;
    private final JoyPad joyPad1;
    private final BiConsumer<PPU, JoyPad> gameLoopCallback;
    //记录当前ppu模块是否触发NMI
    private final AtomicBoolean ppuIsNMI = new AtomicBoolean(false);

    public Bus(byte[] rpg, final PPU ppu, BiConsumer<PPU, JoyPad> gameLoopCallback, JoyPad joyPad, JoyPad joyPad1) {
        this.ppu = ppu;
        this.joyPad = joyPad;
        this.joyPad1 = joyPad1;
        this.rpgSize = rpg.length;
        this.buffer = new byte[0x10000];
        //复制rpg-rom到内存映射中
        System.arraycopy(rpg, 0, this.buffer, 0x8000, rpgSize);
        this.gameLoopCallback = gameLoopCallback;

    }

    public Bus(byte[] rpg, final PPU ppu, JoyPad joyPad, JoyPad joyPad1) {
        this(rpg, ppu, null, joyPad, joyPad1);
    }

    /**
     * 从映射地址中获取真实地址
     */
    private int map(int address) {
        //Mapper from 0x800-0x1fff to 0x000-0x7ff
        if (0x800 <= address && address <= 0x1fff) {
            address &= 0b11111111111;
        }
        //RGB-ROM mapper
        if (address >= 0xc000 && rpgSize == 0x4000) {
            address &= 0b1011_1111_1111_1111;
        }
        //IO_Mirrors mapper
        if (address >= 0x2008 && address <= 0x3fff) {
            address &= 0b10000000000111;
        }
        return address;
    }


    /**
     * 获取当前rpg大小
     */
    public int rpgSize() {
        return this.rpgSize;
    }

    @Override
    public byte read(int address) {
        address = this.map(address);
        final byte b;
        if (address == 0x2002) {
            b = this.ppu.readStatus();
        } else if (address == 0x2004) {
            b = this.ppu.readOam();
        } else if (address == 0x2007) {
            b = this.ppu.read(address);
        }
        //player1
        else if (address == 0x4016) {
            b = this.joyPad.read();
        }
        //player2
        else if (address == 0x4017) {
            b = this.joyPad1.read();
        }
        //apu only write register
        else if (address >= 0x4000 && address <= 0x4013) {
            b = 0;
        }
        //Read from apu
        else if (address == 0x4015) {
            b = 0;
        }
        //only write memory area
        else if (address == 0x2006
                || address == 0x4014
                || address == 0x2005
                || address == 0x2003
                || address == 0x2001
                || address == 0x2000) {
            b = 0;
        } else {
            b = this.buffer[address];
        }
        log.debug("From 0x{} read a byte 0x{}", Integer.toHexString(address), Integer.toHexString(b & 0xff));
        return b;
    }

    public boolean pollPPUNMI() {
        return this.ppuIsNMI.getAndSet(false);
    }

    /**
     * 向内存中写入一字节数据
     */
    public void write(int address, byte b) {
        address = this.map(address);

        log.debug("Write byte {} to target address 0x{}", b, Integer.toHexString(address));

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Controller_($2000)_%3E_write
        if (address == 0x2000) {
            this.ppu.writeCtr(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Mask_($2001)_%3E_write
        else if (address == 0x2001) {
            this.ppu.writeMask(b);
        }
        //https://www.nesdev.org/wiki/PPU_programmer_reference#Status_($2002)_%3C_read
        else if (address == 0x2002) {

        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_address_($2003)_%3E_write
        else if (address == 0x2003) {
            this.ppu.writeOamAddr(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_data_($2004)_%3C%3E_read/write
        else if (address == 0x2004) {
            this.ppu.writeOamByte(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Scroll_($2005)_%3E%3E_write_x2
        else if (address == 0x2005) {
            this.ppu.writeScroll(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Address_($2006)_%3E%3E_write_x2
        else if (address == 0x2006) {
            this.ppu.writeAddr(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Data_($2007)_%3C%3E_read/write
        else if (address == 0x2007) {
            this.ppu.write(address, b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_DMA_($4014)_%3E_write
        else if (address == 0x4014) {
            var buffer = new byte[0x100];
            var msb = Byte.toUnsignedInt(b) << 8;
            for (int i = 0; i < 0x100; i++) {
                buffer[i] = this.read(msb + i);
            }
            this.ppu.writeOam(buffer);
        }
        //Write to standard controller
        else if (address == 0x4016) {
            this.joyPad.write(b);
        } else if (address == 0x4017) {
            this.joyPad1.write(b);
        }
        //Write data to apu
        else if ((address >= 0x4000 && address<=0x4013) || address == 0x4015) {
            //to do write to ppu

        }
        //Write to cpu memory
        else {
            this.buffer[address] = b;
        }
    }

    public void tick(int cycle) {
        //PPU时钟是CPU时钟的3倍
        this.ppu.tick(cycle * 3);
        var nmi = this.ppu.isNMI();
        if (!this.ppuIsNMI.get() && nmi && gameLoopCallback != null) {
            this.gameLoopCallback.accept(this.ppu, this.joyPad);
        }
        this.ppuIsNMI.set(nmi);
    }
}
