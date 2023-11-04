package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.ppu.PPU;
import cn.navclub.nes4j.bin.util.BinUtil;
import lombok.Getter;

import java.util.Arrays;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;
import static cn.navclub.nes4j.bin.util.BinUtil.uint8;
import static cn.navclub.nes4j.bin.util.MathUtil.u8add;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class Bus implements Component {
    private static final int RPG_ROM_START = 0x8000;
    private static final int RPG_ROM_END = 0xFFFF;
    private static final int RAM_MIRROR_END = 0x1fff;
    private static final LoggerDelegate log = LoggerFactory.logger(Bus.class);
    private final NES context;
    @Getter
    private final byte[] ram;
    //Player1
    private final JoyPad joyPad;
    //Player2
    private final JoyPad joyPad1;
    @Getter
    private final PPU ppu;
    private final APU apu;
    //$4020-0x6000 expansion rom
    private final byte[] exp;
    //  SRAM (WRAM) [$6000,$8000) is the Save RAM, the addresses used to access RAM in the cartridges
    //  for storing save games.
    private final byte[] sram;

    public Bus(NES context, JoyPad joyPad, JoyPad joyPad1) {
        this.context = context;

        this.joyPad = joyPad;
        this.joyPad1 = joyPad1;

        this.ram = new byte[2048];
        this.exp = new byte[0x1fe0];
        this.sram = new byte[0x2000];

        this.apu = context.getApu();
        this.ppu = context.getPpu();

        this.reset();
    }

    /**
     * Mapper to real memory address
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
        } else if (address <= 0x2007) {
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
        //Read a byte from expansion rom
        else if (address >= 0x4020 && address < 0x6000) {
            b = this.exp[address - 0x4020];
        }
        //Read a byte from sram
        else if (address >= 0x6000 && address < 0x8000) {
            b = this.sram[address - 0x6000];
        }
        //Read rpg-rom data
        else if (address >= RPG_ROM_START && address <= RPG_ROM_END) {
            b = this.context.getMapper().PRGRead(address - RPG_ROM_START);
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
        //Writer ppu inner register
        else if (address <= 0x2007) {
            this.ppu.write(address, b);
        }
        //https://www.nesdev.org/wiki/PPU_programmer_reference#OAM_DMA_($4014)_%3E_write
        else if (address == 0x4014) {
            this.ppu.dmcWrite(b);
        }
        //Write to standard controller
        else if (address == 0x4016) {
            this.joyPad.write(b);
        }
        //Write data to apu
        else if ((address >= 0x4000 && address <= 0x4013) || address == 0x4015 || address == 0x4017) {
            this.apu.write(address, b);
        }
        //Write a byte to expansion rom
        else if (address >= 0x4020 && address < 0x6000) {
            this.exp[address - 0x4020] = b;
        }
        //Write a byte to sram
        else if (address >= 0x6000 && address < 0x8000) {
            this.sram[address - 0x6000] = b;
        }
        //Write to cpu memory
        else if (address >= RPG_ROM_START && address <= RPG_ROM_END) {
            this.context.getMapper().PRGWrite(address, b);
        }

        //Unknown action
        else {
            log.warning("Unknown bus action write to 0x{}", BinUtil.toHexStr(address));
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
    public void reset() {
        Arrays.fill(this.ram, (byte) 0);
        Arrays.fill(this.exp, (byte) 0);
        Arrays.fill(this.sram, (byte) 0);
    }
}
