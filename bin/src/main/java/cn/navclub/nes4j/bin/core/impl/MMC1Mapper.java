package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

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

    private int prgMode;
    private int chrMode;
    private int bankNum;


    public MMC1Mapper(Cartridge cartridge, NesConsole console) {
        super(cartridge, console);
        this.reset();
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
     * <b>Examples:</b>
     * <pre>
     * {@code
     * ;
     * ; Sets the switchable PRG ROM bank to the value of A.
     * ;
     *               ;  A          MMC1_SR  MMC1_PB
     * setPRGBank:   ;  000edcba    10000             Start with an empty shift register (SR).  The 1 is used
     *   sta $E000   ;  000edcba -> a1000             to detect when the SR has become full.
     *   lsr a       ; >0000edcb    a1000
     *   sta $E000   ;  0000edcb -> ba100
     *   lsr a       ; >00000edc    ba100
     *   sta $E000   ;  00000edc -> cba10
     *   lsr a       ; >000000ed    cba10
     *   sta $E000   ;  000000ed -> dcba1             Once a 1 is shifted into the last position, the SR is full.
     *   lsr a       ; >0000000e    dcba1
     *   sta $E000   ;  0000000e    dcba1 -> edcba    A write with the SR full copies D0 and the SR to a bank register
     *               ;              10000             ($E000-$FFFF means PRG bank number) and then clears the SR.
     *   rts
     * }
     * </pre>
     *
     * @param address Target address
     * @param b       Write target address value
     */
    @Override
    public void PRGWrite(int address, byte b) {
        if ((b & 0x80) == 0x80) {
            this.MMC1SR = 0b10000;
            return;
        }

        var temp = this.MMC1SR;

        this.MMC1SR >>= 1;
        this.MMC1SR |= ((uint8(b) & 0x01) << 4);

        //MMC1SR is full
        if ((temp & 0x01) == 0) {
            return;
        }

        var internal = RInternal.values()[(address >> 13) & 0x03];
        if (internal == RInternal.CONTROL) {

            var value = this.MMC1SR & 3;
            var mirror = switch (value) {
                case 0 -> NameMirror.ONE_SCREEN_LOWER;
                case 1 -> NameMirror.ONE_SCREEN_UPPER;
                case 2 -> NameMirror.VERTICAL;
                default -> NameMirror.HORIZONTAL;
            };
            this.console.getPpu().setMirrors(mirror);

            this.prgMode = (this.MMC1SR >> 2) & 3;
            this.chrMode = (this.MMC1SR >> 4) & 1;
        } else {
            if (internal == RInternal.CHR_BANK0 || internal == RInternal.CHR_BANK1) {
                this.checkUpdateChr(internal, MMC1SR);
            } else {
                this.bankNum = this.MMC1SR;
            }
        }
        this.MMC1SR = 0b10000;
    }

    /**
     * <b>CHR bank 0 (internal, $A000-$BFFF)</b>
     * <pre>
     * {@code
     * 4bit0
     * -----
     * CCCCC
     * |||||
     * +++++- Select 4 KB or 8 KB CHR bank at PPU $0000 (low bit ignored in 8 KB mode)
     * }
     * </pre>
     * <p>
     * MMC1 can do CHR banking in 4KB chunks. Known carts with CHR RAM have 8 KiB, so that makes 2 banks.
     * RAM vs ROM doesn't make any difference for address lines. For carts with 8 KiB of CHR (be it ROM or RAM),
     * MMC1 follows the common behavior of using only the low-order bits: the bank number is in effect ANDed with 1.
     * </p>
     * <b>CHR bank 1 (internal, $C000-$DFFF)</b>
     * <pre>
     * {@code
     * 4bit0
     * -----
     * CCCCC
     * |||||
     * +++++- Select 4 KB CHR bank at PPU $1000 (ignored in 8 KB mode)
     * }
     * </pre>
     *
     * @param internal
     * @param idx
     */
    private void checkUpdateChr(RInternal internal, int idx) {
        if (this.chrSize() == 0) {
            return;
        }
        var chrSize = CHR_BANK_SIZE / (this.chrMode + 1);
        if (internal == RInternal.CHR_BANK0) {
            if (chrSize == CHR_BANK_SIZE) {
                idx &= 0xfe;
            }
            System.arraycopy(this.getChrom(), idx * chrSize, this.chr, 0, chrSize);
        } else if (this.chrMode == 1) {
            System.arraycopy(this.getChrom(), idx * chrSize, this.chr, 0x1000, chrSize);
        }
    }

    @Override
    public byte PRGRead(int address) {
        var prg = this.getRgbrom();
        //0, 1: switch 32 KB at $8000, ignoring low bit of bank number;
        if (this.prgMode <= 1) {
            var idx = this.bankNum & 0xfe;
            return prg[idx * RPG_BANK_SIZE + address];
        }
        var second = (address >= 0x4000);
        var offset = this.bankNum * RPG_BANK_SIZE;
        //
        // 2: fix first bank at $8000 and switch 16 KB bank at $C000;
        // 3: fix last bank at $C000 and switch 16 KB bank at $8000
        //
        if (this.prgMode == 3 && second) {
            offset = getLastBank();
        }
        return prg[offset + address % RPG_BANK_SIZE];
    }

    @Override
    public void reset() {
        this.bankNum = 0;
        this.prgMode = 3;
        this.MMC1SR = 0b10000;
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
