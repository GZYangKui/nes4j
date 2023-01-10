package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;
import cn.navclub.nes4j.bin.util.BinUtil;

import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * <a href="https://www.nesdev.org/wiki/MMC1">
 * <h3>MMC1</h3>
 * </a>
 * <b>Banks</b>
 * <pre>
 * CPU $6000-$7FFF: 8 KB PRG RAM bank, (optional)
 * CPU $8000-$BFFF: 16 KB PRG ROM bank, either switchable or fixed to the first bank
 * CPU $C000-$FFFF: 16 KB PRG ROM bank, either fixed to the last bank or switchable
 * PPU $0000-$0FFF: 4 KB switchable CHR bank
 * PPU $1000-$1FFF: 4 KB switchable CHR bank
 * Through writes to the MMC1 control register, it is possible for the program to swap the fixed and switchable
 * PRG ROM banks or to set up 32 KB PRG bankswitching (like BNROM), but most games use the default setup, which
 * is similar to that of UxROM.
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class MMC1Mapper extends Mapper {
    //Shifter
    private int MMC1SR;
    //Temp register
    private int temp;
    //Control register
    private int control;


    public MMC1Mapper(Cartridge cartridge, NES context) {
        super(cartridge, context);
        this.MMC1SR = 0b10000;
        System.arraycopy(cartridge.getRgbrom(), 0, this.rom, 0, RPG_BANK_SIZE);
        System.arraycopy(cartridge.getRgbrom(), getLastBank(), this.rom, RPG_BANK_SIZE, RPG_BANK_SIZE);
    }

    /**
     * <b>Interface</b>
     * <pre>
     * Unlike almost all other mappers, the MMC1 is configured through a serial port in order to reduce its pin count.
     * CPU $8000-$FFFF is connected to a common shift register. Writing a value with bit 7 set ($80 through $FF) to any
     * address in $8000-$FFFF clears the shift register to its initial state. To change a register's value, the CPU
     * writes five times with bit 7 clear and one bit of the desired value in bit 0 (starting with the low bit of the
     * value). On the first four writes, the MMC1 shifts bit 0 into a shift register. On the fifth write, the MMC1
     * copies bit 0 and the shift register contents into an internal register selected by bits 14 and 13 of the address,
     * and then it clears the shift register. Only on the fifth write does the address matter, and even then, only bits
     * 14 and 13 of the address matter because the mapper doesn't see the lower address bits (similar to the mirroring
     * seen with PPU registers). After the fifth write, the shift register is cleared automatically, so writing again
     * with bit 7 set to clear the shift register is not needed.
     * </pre>
     *
     * <b>Control register</b>
     * <pre>
     * 4bit0
     * -----
     * CPPMM
     * |||||
     * |||++- Mirroring (0: one-screen, lower bank; 1: one-screen, upper bank;
     * |||               2: vertical; 3: horizontal)
     * |++--- PRG ROM bank mode (0, 1: switch 32 KB at $8000, ignoring low bit of bank number;
     * |                         2: fix first bank at $8000 and switch 16 KB bank at $C000;
     * |                         3: fix last bank at $C000 and switch 16 KB bank at $8000)
     * +----- CHR ROM bank mode (0: switch 8 KB at a time; 1: switch two separate 4 KB banks)
     * </pre>
     *
     * @param address Target address
     * @param b       Write target address value
     */
    @Override
    public void writeRom(int address, byte b) {
        if ((b & 0x80) == 0x80) {
            this.MMC1SR = 0b10000;
            return;
        }

        var temp = this.MMC1SR;

        this.MMC1SR >>= 1;
        this.MMC1SR |= ((uint8(b) & 0x01) << 4);

        var internal = RInternal.values()[(address >> 13) & 0x03];
        //MMC1SR is full
        if ((temp & 0x01) == 0x01) {
            if (internal == RInternal.CONTROL) {
                this.control = MMC1SR;
            } else {
                this.temp = MMC1SR;
            }
            this.MMC1SR = 0b10000;
            if (internal != RInternal.CONTROL) {
                this.calculate(internal);
            } else {
                var value = this.control & 0x03;
                if (value == 2) {
                    this.context.getPpu().setMirrors(NameMirror.VERTICAL);
                }
                if (value == 3) {
                    this.context.getPpu().setMirrors(NameMirror.HORIZONTAL);
                }
            }
        }
    }

    private void calculate(RInternal internal) {
        //Switch PRG bank
        if (internal == RInternal.PRG_BANK) {
            var offset = this.temp * RPG_BANK_SIZE;
            var mode = (this.control >> 2) & 0x03;
            //(0, 1: switch 32 KB at $8000, ignoring low bit of bank number;
            if ((mode | 1) <= 1) {
                System.arraycopy(
                        this.cartridge.getRgbrom(),
                        offset,
                        this.rom,
                        0,
                        2 * RPG_BANK_SIZE
                );
            }
            // 2: fix first bank at $8000 and switch 16 KB bank at $C000;
            else if (mode == 2) {
                System.arraycopy(this.cartridge.getRgbrom(), 0, this.rom, 0, RPG_BANK_SIZE);
                System.arraycopy(this.cartridge.getRgbrom(), offset, this.rom, RPG_BANK_SIZE, RPG_BANK_SIZE);
            }
            //3: fix last bank at $C000 and switch 16 KB bank at $8000)
            else {
                System.arraycopy(this.cartridge.getRgbrom(), getLastBank(), this.rom, RPG_BANK_SIZE, RPG_BANK_SIZE);
                System.arraycopy(this.cartridge.getRgbrom(), offset, this.rom, 0, RPG_BANK_SIZE);
            }
        } else {
            var mode = (this.control >> 4) & 0x01;
            //
            // 4bit0
            // -----
            // CCCCC
            // |||||
            // +++++- Select 4 KB or 8 KB CHR bank at PPU $0000 (low bit ignored in 8 KB mode)
            //
            if (internal == RInternal.CHR_BANK0 && mode == 1) {
                System.arraycopy(this.cartridge.getChrom(),
                        temp * CHR_BANK_SIZE, this.com, 0, CHR_BANK_SIZE);
            }
            //
            // 4bit0
            // -----
            // CCCCC
            // |||||
            // +++++- Select 4 KB CHR bank at PPU $1000 (ignored in 8 KB mode)
            //
            if (internal == RInternal.CHR_BANK1 && mode == 1) {
                System.arraycopy(this.cartridge.getChrom(),
                        temp * CHR_BANK_SIZE / 2, this.com, 0x1000, CHR_BANK_SIZE / 2);
            }
        }
    }

    private enum RInternal {
        //Control (internal, $8000-$9FFF)
        CONTROL,
        //CHR bank 0 (internal, $A000-$BFFF)
        CHR_BANK0,
        //CHR bank 1 (internal, $C000-$DFFF)
        CHR_BANK1,
        //PRG bank (internal, $E000-$FFFF)
        PRG_BANK
    }
}
