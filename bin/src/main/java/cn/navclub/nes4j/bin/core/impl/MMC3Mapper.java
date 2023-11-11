package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;

import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * <a href="https://www.nesdev.org/wiki/MMC3">MMC3</a>
 *
 * <h3>Bank</h3>
 *
 * <li>CPU $6000-$7FFF: 8 KB PRG RAM bank (optional)</li>
 * <li>CPU $8000-$9FFF (or $C000-$DFFF): 8 KB switchable PRG ROM bank</li>
 * <li>CPU $A000-$BFFF: 8 KB switchable PRG ROM bank</li>
 * <li>CPU $C000-$DFFF (or $8000-$9FFF): 8 KB PRG ROM bank, fixed to the second-last bank</li>
 * <li>CPU $E000-$FFFF: 8 KB PRG ROM bank, fixed to the last bank</li>
 * <li>PPU $0000-$07FF (or $1000-$17FF): 2 KB switchable CHR bank</li>
 * <li>PPU $0800-$0FFF (or $1800-$1FFF): 2 KB switchable CHR bank</li>
 * <li>PPU $1000-$13FF (or $0000-$03FF): 1 KB switchable CHR bank</li>
 * <li>PPU $1400-$17FF (or $0400-$07FF): 1 KB switchable CHR bank</li>
 * <li>PPU $1800-$1BFF (or $0800-$0BFF): 1 KB switchable CHR bank</li>
 * <li>PPU $1C00-$1FFF (or $0C00-$0FFF): 1 KB switchable CHR bank</li>
 *
 * <h3>Register</h3>
 * The MMC3 has 4 pairs of registers at $8000-$9FFF, $A000-$BFFF, $C000-$DFFF, and $E000-$FFFF - even
 * addresses ($8000, $8002, etc.) select the low register and odd addresses ($8001, $8003, etc.) select the
 * high register in each pair. These can be broken into two independent functional units: memory
 * mapping ($8000, $8001, $A000, $A001) and scanline counting ($C000, $C001, $E000, $E001).
 */
public class MMC3Mapper extends Mapper {
    private static final int PRG_BANK_BANK = 8 * 1024;
    private int r;
    private int pbm;
    private int latch;
    private int counter;
    private int chrInversion;
    private boolean IRQEnable;
    private boolean reloadFlag;
    private final int[] PRGBank;
    /**
     * PPU $0000-$07FF (or $1000-$17FF): 2 KB switchable CHR bank
     * PPU $0800-$0FFF (or $1800-$1FFF): 2 KB switchable CHR bank
     * PPU $1000-$13FF (or $0000-$03FF): 1 KB switchable CHR bank
     * PPU $1400-$17FF (or $0400-$07FF): 1 KB switchable CHR bank
     * PPU $1800-$1BFF (or $0800-$0BFF): 1 KB switchable CHR bank
     * PPU $1C00-$1FFF (or $0C00-$0FFF): 1 KB switchable CHR bank
     */
    private final int[] CHRBank;

    private final int PRGMode;

    public MMC3Mapper(Cartridge cartridge, NesConsole console) {
        super(cartridge, console);
        this.r = 0;
        this.latch = 0;
        this.counter = 0;
        this.IRQEnable = false;
        this.reloadFlag = false;
        this.PRGBank = new int[4];
        this.CHRBank = new int[8];

        if (this.chrSize() == 0) {
            for (int i = 0; i < 8; i++) {
                this.CHRBank[i] = i;
            }
        }

        //
        // Because the values in R6, R7, and $8000 are unspecified at power on, the reset vector must point
        // into $E000-$FFFF,and code must initialize these before jumping out of $E000-$FFFF.
        //
        this.PRGMode = calMaxBankIdx(PRG_BANK_BANK);
        this.PRGBank[3] = this.PRGMode;
        this.PRGBank[2] = this.PRGMode - 1;
    }

    @Override
    public void PRGWrite(int address, byte b) {
        //CPU $6000-$7FFF: 8 KB PRG RAM bank (optional)
        var even = (address & 1) == 0;
        //
        // Mirroring ($A000-$BFFE, even)
        // 7  bit  0
        // ---- ----
        // xxxx xxxM
        //         |
        //         +- Nametable mirroring (0: vertical; 1: horizontal)
        //
        // This bit has no effect on cartridges with hardwired 4-screen VRAM. In the iNES and NES 2.0 formats, this can be identified through bit 3 of byte $06 of the header.
        if (even && address >= 0xA000 && address <= 0xBFFF && this.cartridge.getMirrors() != NameMirror.FOUR_SCREEN) {
            this.console.getPpu().setMirrors((b & 1) == 1 ? NameMirror.HORIZONTAL : NameMirror.VERTICAL);
        }

        if (address <= 0x9FFF) {
            // Bank select ($8000-$9FFE, even)
            // 7  bit  0
            // ---- ----
            // CPMx xRRR
            // |||   |||
            // |||   +++- Specify which bank register to update on next write to Bank Data register
            // |||          000: R0: Select 2 KB CHR bank at PPU $0000-$07FF (or $1000-$17FF)
            // |||          001: R1: Select 2 KB CHR bank at PPU $0800-$0FFF (or $1800-$1FFF)
            // |||          010: R2: Select 1 KB CHR bank at PPU $1000-$13FF (or $0000-$03FF)
            // |||          011: R3: Select 1 KB CHR bank at PPU $1400-$17FF (or $0400-$07FF)
            // |||          100: R4: Select 1 KB CHR bank at PPU $1800-$1BFF (or $0800-$0BFF)
            // |||          101: R5: Select 1 KB CHR bank at PPU $1C00-$1FFF (or $0C00-$0FFF)
            // |||          110: R6: Select 8 KB PRG ROM bank at $8000-$9FFF (or $C000-$DFFF)
            // |||          111: R7: Select 8 KB PRG ROM bank at $A000-$BFFF
            // ||+------- Nothing on the MMC3, see MMC6
            // |+-------- PRG ROM bank mode (0: $8000-$9FFF swappable,
            // |                                $C000-$DFFF fixed to second-last bank;
            // |                             1: $C000-$DFFF swappable,
            // |                                $8000-$9FFF fixed to second-last bank)
            // +--------- CHR A12 inversion (0: two 2 KB banks at $0000-$0FFF,
            //                                 four 1 KB banks at $1000-$1FFF;
            //                              1: two 2 KB banks at $1000-$1FFF,
            //                                 four 1 KB banks at $0000-$0FFF)
            if (even) {
                this.r = b & 0x07;
                this.pbm = (b >> 6) & 1;
                this.chrInversion = (b >> 7) & 1;
            }
            //
            // Bank data ($8001-$9FFF, odd)
            //
            // 7  bit  0
            // ---- ----
            // DDDD DDDD
            // |||| ||||
            // ++++-++++- New bank value, based on last value written to Bank select register (mentioned above)
            // R6 and R7 will ignore the top two bits, as the MMC3 has only 6 PRG ROM address lines. Some romhacks
            // rely on an 8-bit extension of R6/7 for oversized PRG-ROM, but this is deliberately not supported by
            // many emulators. See iNES Mapper 004 below.
            //
            // R0 and R1 ignore the bottom bit, as the value written still counts banks in 1KB units but odd numbered banks can't be selected.
            //
            else {
                if (this.r > 5) {
                    this.PRGSwap(b);
                } else {
                    this.ChrSwap(b);
                }
            }
        }
        //
        // IRQ latch ($C000-$DFFE, even)
        //
        //  7  bit  0
        //  ---- ----
        //  DDDD DDDD
        //  |||| ||||
        //  ++++-++++- IRQ latch value
        //
        // This register specifies the IRQ counter reload value. When the IRQ counter is zero (or a reload is
        // requested through $C001), this value will be copied to the IRQ counter at the NEXT rising edge of
        // the PPU address, presumably at PPU cycle 260 of the current scanline.
        //
        if (address >= 0xC000 && address <= 0xDFFE && even) {
            this.latch = uint8(b);
        }

        //
        // IRQ reload ($C001-$DFFF, odd)
        //
        //  7  bit  0
        //  ---- ----
        //  xxxx xxxx
        //
        // Writing any value to this register clears the MMC3 IRQ counter immediately, and then reloads it
        // at the NEXT rising edge of the PPU address, presumably at PPU cycle 260 of the current scanline.
        //
        if (address >= 0xC001 && address <= 0xDFFF && !even) {
            this.counter = 0;
            this.reloadFlag = true;
        }

        // odd: Writing any value to this register will enable MMC3 interrupts.
        // even: Writing any value to this register will disable MMC3 interrupts AND acknowledge any pending interrupts.
        if (address >= 0xe000 && address <= 0xffff) {
            this.IRQEnable = !even;
        }

        // PRG RAM protect ($A001-$BFFF, odd)
        // 7  bit  0
        // ---- ----
        // RWXX xxxx
        // ||||
        // ||++------ Nothing on the MMC3, see MMC6
        // |+-------- Write protection (0: allow writes; 1: deny writes)
        // +--------- PRG RAM chip enable (0: disable; 1: enable)
        //
        // Note: Though these bits are functional on the MMC3, their main purpose is to write-protect save
        // RAM during power-off. Many emulators choose not to implement them as part of iNES Mapper 4 to avoid
        // an incompatibility with the MMC6.
    }

    private void PRGSwap(byte b) {
        //
        // R6 and R7 will ignore the top two bits, as the MMC3 has only 6 PRG ROM address lines.
        //
        // PRG map mode → $8000.D6 = 0	$8000.D6 = 1
        //  CPU Bank	  Value  of MMC3 register
        // $8000-$9FFF	  R6	        (-2)
        // $A000-$BFFF	  R7	        R7
        // $C000-$DFFF	  (-2)	        R6
        // $E000-$FFFF	  (-1)	        (-1)
        //
        var offset = (b & 0x3f) & this.PRGMode;
        if (this.r == 7) {
            this.PRGBank[1] = offset;
        } else {
            this.PRGBank[this.pbm << 1] = offset;
            this.PRGBank[((this.pbm ^ 0xff) << 1) & 0x03] = this.PRGMode - 1;
        }
    }

    private void ChrSwap(byte b) {
        // R0 and R1 ignore the bottom bit, as the value written still counts banks in 1KB units but odd numbered
        // banks can't be selected.
        if (this.r < 2) {
            b &= 0b1111110;
        }
        //
        //
        //  CHR map mode →	$8000.D7 = 0	$8000.D7 = 1
        //  PPU Bank	Value of MMC3 register
        //  $0000-$03FF	    R0	                R2
        //  $0400-$07FF	                        R3
        //  $0800-$0BFF	    R1	                R4
        //  $0C00-$0FFF	                        R5
        //  $1000-$13FF	    R2	                R0
        //  $1400-$17FF	    R3
        //  $1800-$1BFF	    R4	                R1
        //  $1C00-$1FFF	    R5
        //
        var index = uint8(b);
        if (this.r == 0) {
            var k = this.chrInversion << 2;
            this.CHRBank[k] = index;
            this.CHRBank[k + 1] = index + 1;
        } else if (this.r == 1) {
            var k = 2 | (this.chrInversion << 2);
            this.CHRBank[k] = index;
            this.CHRBank[k + 1] = index + 1;
        } else {
            var k = this.chrInversion == 0 ? 2 : -2;
            this.CHRBank[k + r] = index;
        }
    }

    @Override
    public byte PRGRead(int address) {
        var idx = address / PRG_BANK_BANK;
        var offset = address % PRG_BANK_BANK;
        return super.PRGRead(this.PRGBank[idx] * 0x2000 + offset);
    }

    @Override
    public byte CHRead(int address) {
        var index = address / 0x400;
        var offset = address % 0x400;
        return this.getChrom()[this.CHRBank[index] * 0x400 + offset];
    }

    @Override
    public void tick() {
        // When the IRQ is clocked (filtered A12 0→1), the counter value is checked - if zero
        // or the reload flag is true, it's reloaded with the IRQ latched value at $C000; otherwise,
        // it decrements.
        if (this.counter == 0 || reloadFlag) {
            this.counter = this.latch;
            this.reloadFlag = false;
        } else {
            this.counter--;
        }

        // If the IRQ counter is zero and IRQs are enabled ($E001), an IRQ is triggered.
        // The "alternate revision" checks the IRQ counter transition 1→0, whether from
        // decrementing or reloading.
        if (this.counter == 0 && this.IRQEnable) {
            this.console.hardwareInterrupt(CPUInterrupt.IRQ);
        }
    }
}
