package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.Component;
import cn.navclub.nes4j.bin.enums.CPUInterrupt;
import cn.navclub.nes4j.bin.enums.NMapper;
import cn.navclub.nes4j.bin.enums.NameMirror;
import cn.navclub.nes4j.bin.function.TCallback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Bus implements Component {
    private static final int RPG_ROM = 0x8000;
    private static final int RPG_ROM_END = 0xFFFF;
    private static final int RAM_MIRROR_END = 0x1fff;
    private static final int RPG_UNIT = 16 * 1024;
    //CPU clock cycle counter
    @Getter
    private long cycle;
    @Getter
    private final APU apu;
    @Getter
    private final PPU ppu;
    private final byte[] ram;
    //Dynamic change rpg rom
    private final byte[] rpgrom;
    //Player1
    private final JoyPad joyPad;
    //Player2
    private final JoyPad joyPad1;
    @Getter
    private final TCallback<PPU, JoyPad, JoyPad> gameLoopCallback;
    @Setter
    private CPU cpu;
    private final Cartridge cartridge;

    public Bus(Cartridge cartridge, TCallback<PPU, JoyPad, JoyPad> gameLoopCallback) {

        this.ram = new byte[2048];
        this.joyPad = new JoyPad();
        this.cartridge = cartridge;
        this.joyPad1 = new JoyPad();
        this.apu = new APU(this);
        this.rpgrom = new byte[RPG_UNIT * 2];


        this.ppu = new PPU(this, cartridge.getChrom(), cartridge.getMirrors());
        if (cartridge.getMapper() == NMapper.NROM) {
            System.arraycopy(this.cartridge.getRgbrom(), 0, this.rpgrom, 0, RPG_UNIT);
        }

        System.arraycopy(this.cartridge.getRgbrom(), ((this.rpgSize() / RPG_UNIT) - 1) * RPG_UNIT, rpgrom, RPG_UNIT, RPG_UNIT);

        this.gameLoopCallback = gameLoopCallback;
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

    private byte readRPGData(int address) {
        address -= 0x8000;
        if (rpgSize() == 0x4000 && address >= 0x4000) {
            address %= 0x4000;
        }
        return this.rpgrom[address];
    }


    /**
     * Get Cartridge rpg-rom size
     */
    public int rpgSize() {
        return this.rpgrom.length;
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
//            b = this.joyPad1.read();
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
            b = this.readRPGData(address);
        }
        //Default return 0
        else {
            b = 0;
        }
        return b;
    }

    /**
     * 向内存中写入一字节数据
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
            this.ppu.writeMask(b);
        }
        //https://www.nesdev.org/wiki/PPU_programmer_reference#Status_($2002)_%3C_read
        else if (address == 0x2002) {
            throw new RuntimeException("Attempt write only read ppu register.");
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
        }

        //Write data to apu
        else if ((address >= 0x4000 && address <= 0x4013) || address == 0x4015 || address == 0x4017) {
            this.apu.write(address, b);
        }

        //Write to cpu memory
        else if (address >= RPG_ROM && address <= RPG_ROM_END) {
            var mapper = this.cartridge.getMapper();
            if (mapper == NMapper.NROM)
                throw new RuntimeException("RPG-ROM belong only memory area.");
            else if (mapper == NMapper.UX_ROM)
                System.arraycopy(this.rpgrom, (b & 0xff) * RPG_UNIT, this.rpgrom, 0, RPG_UNIT);
            else
                throw new RuntimeException("un-support mapper:" + mapper + "");
        }

    }

    /**
     * 向指定位置写入无符号字节
     */
    public void writeUSByte(int address, int value) {
        this.write(address, (byte) value);
    }

    /**
     * 向目标地址写入双字节
     */
    public void writeInt(int address, int value) {
        var lsb = (byte) (value & 0xff);
        var msb = (byte) (value >> 8 & 0xff);
        this.write(address, lsb);
        this.write(address + 1, msb);
    }

    /**
     * 读无符号字节
     */
    public int readUSByte(int address) {
        return this.read(address) & 0xff;
    }

    /**
     * 以小端序形式读取数据
     */
    public int readInt(int address) {
        var lsb = this.read(address);
        var msb = this.read(address + 1);
        return (lsb & 0xff) | ((msb & 0xff) << 8);
    }


    @Override
    public void tick(int cycle) {
        this.cycle += cycle;
        for (int i = 0; i < cycle; i++) {
            this.ppu.tick();
            this.apu.tick();
        }
    }

    /**
     *
     * {@link APU} AND {@link  PPU} trigger IRQ AND NMI interrupt.
     *
     * @param interrupt interrupt type
     */
    public void interrupt(CPUInterrupt interrupt) {
        if (interrupt == CPUInterrupt.NMI && this.gameLoopCallback != null) {
            this.gameLoopCallback.accept(this.ppu, this.joyPad, this.joyPad1);
        }
    }
}
