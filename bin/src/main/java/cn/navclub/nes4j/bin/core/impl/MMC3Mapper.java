package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NES;
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
 */
public class MMC3Mapper extends Mapper {
    private static final int PRG_BANK_BANK = 8 * 1024;
    private int r;
    private int latch;

    private int pbm;
    private int chrInversion;

    private boolean clip;
    private final byte[] prg;
    private boolean IRQEnable;


    public MMC3Mapper(Cartridge cartridge, NES context) {
        super(cartridge, context);
        this.r = 0;
        this.latch = 0;
        this.clip = false;
        this.prg = new byte[PRG_BANK_BANK * 4];
        //Fix the last bank to $E000-$FFFF
        System.arraycopy(cartridge.getRgbrom(), this.getLastBank(PRG_BANK_BANK), this.prg, PRG_BANK_BANK * 3, PRG_BANK_BANK);
    }

    @Override
    public void PRGWrite(int address, byte b) {
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
        if (even && address >= 0xa000 && address <= 0xbfff
                && this.cartridge.getMirrors() != NameMirror.FOUR_SCREEN) {
            this.context.getPpu().setMirrors((b & 1) == 1 ? NameMirror.HORIZONTAL : NameMirror.VERTICAL);
        }

        if (address >= 0x8000 && address <= 0x9ffff) {
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
        if (address >= 0xc000 && address <= 0xdffe && even) {
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
        if (address >= 0xc001 && address <= 0xdfff && !even) {
            this.latch = 0;
        }

        // even: Writing any value to this register will disable MMC3 interrupts AND acknowledge any pending interrupts.
        // odd: Writing any value to this register will enable MMC3 interrupts.
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
        if (address >= 0xA001 && address <= 0xBFFF && !even) {
            this.clip = ((b >> 7 & 1) == 1);
        }
    }

    private void PRGSwap(byte b) {
        var offset = uint8(b) * PRG_BANK_BANK;
        if (this.r == 7) {
            System.arraycopy(this.getRgbrom(), offset, this.prg, PRG_BANK_BANK, PRG_BANK_BANK);
        }
        if (this.r == 6) {
            var swapIdx = this.pbm == 0 ? 0 : 1;
            System.arraycopy(this.getRgbrom(), offset, this.prg, swapIdx * PRG_BANK_BANK, PRG_BANK_BANK);
            var sdl = getLastBank(PRG_BANK_BANK) - PRG_BANK_BANK;
            if (swapIdx == 0) {
                System.arraycopy(this.getRgbrom(), sdl, this.prg, PRG_BANK_BANK, PRG_BANK_BANK);
            } else {
                System.arraycopy(this.getRgbrom(), sdl, this.prg, 0, PRG_BANK_BANK);
            }
        }
    }

    private void ChrSwap(byte b) {
        // R0 and R1 ignore the bottom bit, as the value written still counts banks in 1KB units but odd numbered
        // banks can't be selected.
        if (this.r < 2) {
            b &= 0b1111110;
        }
        var offset = uint8(b) * 1024;
        if (this.chrInversion == 0) {
            if (this.r < 2) {
                System.arraycopy(this.getChrom(), offset, this.chr, this.r * 2048, 2048);
            } else {
                System.arraycopy(this.getChrom(), offset, this.chr, (this.r + 2) * 1024, 1024);
            }
        } else {
            if (this.r == 0 || this.r == 1) {
                System.arraycopy(this.getChrom(), offset, this.chr, 4096 + (this.r * 2048), 2048);
            } else {
                System.arraycopy(this.getChrom(), offset, this.chr, (this.r - 2) * 1024, 1024);
            }
        }
    }

    @Override
    public byte PRGRead(int address) {
        return prg[address];
    }
}
