package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;
import cn.navclub.nes4j.bin.util.BinUtil;

import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
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
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class MMC1Mapper extends Mapper {
    private int MMC1SR;
    //RPG bank
    private int MMC1_PB;


    public MMC1Mapper(Cartridge cartridge) {
        super(cartridge);
        this.MMC1SR = 0;
        System.arraycopy(cartridge.getRgbrom(), 0, this.rom, 0, RPG_UNIT);
        System.arraycopy(cartridge.getRgbrom(), getLastBank(), this.rom, RPG_UNIT, RPG_UNIT);
    }

    @Override
    public void writeRom(int address, byte b) {
        System.out.println("rpg:address:" + address + ",value:" + BinUtil.toHexStr(b));
        if ((b & 0x80) == 0x80) {
            this.MMC1SR = 0;
            this.MMC1_PB = 0;
        } else {
            this.MMC1_PB |= ((uint8(b) & 0x01) << this.MMC1SR);
            if ((++this.MMC1SR) == 5) {
                System.arraycopy(
                        cartridge.getRgbrom(),
                        this.MMC1SR * RPG_UNIT,
                        this.rom,
                        address <= 0xbfff ? 0 : RPG_UNIT,
                        RPG_UNIT
                );

            }
        }
    }

    /**
     * <b>CHR bank 0 (internal, $A000-$BFFF)</b>
     * <pre>
     * 4bit0
     * -----
     * CCCCC
     * |||||
     * +++++- Select 4 KB or 8 KB CHR bank at PPU $0000 (low bit ignored in 8 KB mode)
     *
     * MMC1 can do CHR banking in 4KB chunks. Known carts with CHR RAM have 8 KiB, so that makes 2 banks.
     * RAM vs ROM doesn't make any difference for address lines. For carts with 8 KiB of CHR (be it ROM or RAM),
     * MMC1 follows the common behavior of using only the low-order bits: the bank number is in effect ANDed with 1.
     *
     * </pre>
     * <b>CHR bank 1 (internal, $C000-$DFFF)</b>
     * <pre>
     * 4bit0
     * -----
     * CCCCC
     * |||||
     * +++++- Select 4 KB CHR bank at PPU $1000 (ignored in 8 KB mode)
     * </pre>
     *
     * @param address {@inheritDoc}
     * @param b       {@inheritDoc}
     */
//    @Override
//    public void writeCom(int address, byte b) {
//        if (address >= 0xa000 && address <= 0xbfff) {
//
//        } else {
//            var str = "aaa";
//        }
//        //System.out.println("com:address:" + address + ",value:" + BinUtil.toHexStr(b));
//
//    }
}
