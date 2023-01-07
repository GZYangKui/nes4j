package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.config.NMapper;
import cn.navclub.nes4j.bin.io.Cartridge;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.PPU;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;
import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@Slf4j
public class Bus implements Component {
    private static final int RPG_ROM = 0x8000;
    private static final int RPG_ROM_END = 0xFFFF;
    private static final int RAM_MIRROR_END = 0x1fff;
    private static final int RPG_UNIT = 16 * 1024;
    private final NES context;
    private final byte[] ram;
    //Player1
    private final JoyPad joyPad;
    //Player2
    private final JoyPad joyPad1;
    @Getter
    private final PPU ppu;
    private final APU apu;

    public Bus(NES context, JoyPad joyPad, JoyPad joyPad1) {
        this.context = context;

        this.joyPad = joyPad;
        this.joyPad1 = joyPad1;

        this.ram = new byte[2048];

        this.apu = context.getApu();
        this.ppu = context.getPpu();
    }

    /**
     * 从映射地址中获取真实地址
     */
    private int map(int address) {
        //Mapper from 0x800-0x1fff to 0x000-0x7ff
        if (0x800 <= address && address <= 0x1fff) {
            address &= 0b11111111111;
        }
        //IO_Mirrors mapper
        if (address >= 0x2008 && address <= 0x3fff) {
            address &= 0b10000000000111;
        }
        return address;
    }

    @Override
    public byte read(int address) {
        final byte b;
        address = this.map(address);
        if (address >= 0 && address <= RAM_MIRROR_END) {
            b = this.ram[address];
        } else if (address == 0x2002) {
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
            b = 0;
        }
        //apu only write register
        else if (address >= 0x4000 && address <= 0x4013) {
            b = 0;
        }
        //Read from apu
        else if (address == 0x4015) {
            return this.apu.read(0x4015);
        }
        //only write memory area
        else if (address == 0x2006
                || address == 0x4014
                || address == 0x2005
                || address == 0x2003
                || address == 0x2001
                || address == 0x2000) {
            b = 0;
        }
        //Read rpg-rom data
        else if (address >= RPG_ROM && address <= RPG_ROM_END) {
            b = this.context.getMapper().readRom(address - 0x8000);
        }
        //Default return 0
        else {
            b = 0;
        }
        return b;
    }

    /**
     * Write a byte to target memory address
     *
     * @param address Target memory address
     * @param b       Write data
     */
    public void write(int address, byte b) {
        address = this.map(address);

        if (address >= 0 && address <= RAM_MIRROR_END) {
            this.ram[address] = b;
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Controller_($2000)_%3E_write
        else if (address == 0x2000) {
            this.ppu.writeCtr(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Mask_($2001)_%3E_write
        else if (address == 0x2001) {
            this.ppu.MaskWrite(b);
        }
//        //https://www.nesdev.org/wiki/PPU_programmer_reference#Status_($2002)_%3C_read
//        else if (address == 0x2002) {
//            throw new RuntimeException("Attempt write only read ppu register.");
//        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_address_($2003)_%3E_write
        else if (address == 0x2003) {
            this.ppu.OAMAddrWrite(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_data_($2004)_%3C%3E_read/write
        else if (address == 0x2004) {
            this.ppu.OAMWrite(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Scroll_($2005)_%3E%3E_write_x2
        else if (address == 0x2005) {
            this.ppu.ScrollWrite(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Address_($2006)_%3E%3E_write_x2
        else if (address == 0x2006) {
            this.ppu.AddrWrite(b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#Data_($2007)_%3C%3E_read/write
        else if (address == 0x2007) {
            this.ppu.write(address, b);
        }

        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_DMA_($4014)_%3E_write
        else if (address == 0x4014) {
            var buffer = new byte[0x100];
            var msb = uint8(b) << 8;
            for (int i = 0; i < 0x100; i++) {
                buffer[i] = this.read(msb + i);
            }
            this.ppu.DMAWrite(buffer);
        }
        //Write to standard controller
        else if (address == 0x4016) {
            this.joyPad.write(b);
        }

        //Write data to apu
        else if ((address >= 0x4000 && address <= 0x4013) || address == 0x4015 || address == 0x4017) {
            this.apu.write(address, b);
        }

        //Write to cpu memory
        else if (address >= RPG_ROM && address <= RPG_ROM_END) {
            this.context.getMapper().writeRom(address, b);
        }

    }

    /**
     * Write unsigned data to target memory address
     *
     * @param address Target memory address
     * @param value   Unsigned byte data
     */
    public void WriteU8(int address, int value) {
        this.write(address, int8(value));
    }

    /**
     * Read unsigned data from target memory address
     *
     * @param address Target memory address
     * @return Unsigned byte data
     */
    public int ReadU8(int address) {
        return uint8(this.read(address));
    }

    /**
     * Little endian read continue two memory address value
     *
     * @param address Memory address offset
     * @return Two address memory value
     */
    public int readInt(int address) {
        var lsb = this.read(address);
        var msb = this.read(address + 1);
        return (lsb & 0xff) | ((msb & 0xff) << 8);
    }

    @Override
    public void stop() {
        this.apu.stop();
        this.ppu.stop();
    }

    @Override
    public void tick() {

    }
}
