package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.core.Mapper;
import cn.navclub.nes4j.bin.io.Cartridge;


/**
 * The Konami VRC2 and Konami VRC4 are two related ASIC mappers in the VRC[1] family.
 * <p>
 * Because the VRC2 is mostly a subset of the VRC4, relevant games are often emulated as VRC4 only. iNES mappers 21, 22, 23 and 25 implement various board permutations, and NES 2.0 submappers may be used to disambiguate further.
 * <p>
 * Mapper 27 represents a related pirate mapper.
 */
public class KonamiVRC24 extends Mapper {
    private static final int PRG_SWAP_SIZE = 8192;
    private int chrBank;
    private int swapMode;
    private final int mod;
    private final int chrMode;
    private final int[] PRGMapper;
    private final int[] CHRMapper;

    public KonamiVRC24(Cartridge cartridge, NesConsole console) {
        super(cartridge, console);
        this.swapMode = 0;
        this.PRGMapper = new int[4];
        this.CHRMapper = new int[8];
        this.chrMode = this.chrSize() / 0x400 - 1;
        this.mod = this.PRGMapper[3] = this.calMaxBankIdx(PRG_SWAP_SIZE);
    }

    @Override
    public byte PRGRead(int address) {
        var idx = this.PRGMapper[address / PRG_SWAP_SIZE];
        var offset = address % PRG_SWAP_SIZE;
        return super.PRGRead(idx * PRG_SWAP_SIZE + offset);
    }

    @Override
    public void PRGWrite(int address, byte b) {
        /*
         * PRG Swap Mode/WRAM control ($9002) VRC4
         *
         *  7  bit  0
         *  ---------
         *  .... ..MW
         *         |+- WRAM Control
         *         +-- Swap Mode
         * When 'W' is clear:
         *
         * the 8 KiB page at $6000 is open bus, and WRAM content cannot be read nor written
         * When 'W' is set:
         *
         * the 8 KiB page at $6000 is WRAM, and WRAM content can be read and written
         * When 'M' is clear:
         *
         * the 8 KiB page at $8000 is controlled by the $800x register
         * the 8 KiB page at $C000 is fixed to the second last 8 KiB in the ROM
         * When 'M' is set:
         *
         * the 8 KiB page at $8000 is fixed to the second last 8 KiB in the ROM
         * the 8 KiB page at $C000 is controlled by the $800x register
         */
        if (address == 0x9002) {
            this.swapMode = (b >> 1) & 0xff;
        }
        /*
         *  PRG Select 0 ($8000, $8001, $8002, $8003)
         *
         *  7  bit  0
         *  ---------
         *  ...P PPPP
         *  | ||||
         *  +-++++- Select 8 KiB PRG bank at $8000 or $C000 depending on Swap Mode
         *
         * VRC2 does not have a Swap Mode. The bank is always at $8000.
         *
         */
        if (address > 0x7fff && address < 0x8004) {
            var idx = b & this.mod;
            if (this.swapMode == 0) {
                this.PRGMapper[0] = idx;
                this.PRGMapper[2] = this.PRGMapper[3] - 1;
            } else {
                this.PRGMapper[0] = this.PRGMapper[3] - 1;
                this.PRGMapper[2] = idx;
            }
        }
        /*
         * PRG Select 1 ($A000, $A001, $A002, $A003)
         * 7  bit  0
         * ---------
         *  ...P PPPP
         *  | ||||
         *  +-++++- Select 8 KiB PRG bank at $A000
         */
        if (address > 0x9FFF && address < 0xA004) {
            this.PRGMapper[1] = (b & this.mod);
        }
        /*
         * Mirroring Control ($9000, $9001, $9002, $9003)
         *
         * 7  bit  0
         * ---------
         * .... ..MM
         *        ||
         *        ++- Mirroring (0: vertical; 1: horizontal; 2: one-screen, lower bank; 3: one-screen, upper bank)
         */
        if (address > 0x8fff && address <= 0x9004) {
            var caseVal = b & 0x03;
            var mirror = switch (caseVal) {
                case 0 -> NameMirror.VERTICAL;
                case 1 -> NameMirror.HORIZONTAL;
                case 2 -> NameMirror.ONE_SCREEN_LOWER;
                case 3 -> NameMirror.ONE_SCREEN_UPPER;
                default -> throw new IllegalStateException("Unexpected value: " + caseVal);
            };
            this.console.getPpu().setMirrors(mirror);
        }
        //
        //  8 pairs chr swap register
        //
        if (address == 0xB000 || address == 0xB001) {
            this.ChrSwap(address, b, 0);
        }
        if (address == 0xB002 || address == 0xB003) {
            this.ChrSwap(address, b, 1);
        }
        if (address == 0xC000 || address == 0xC001) {
            this.ChrSwap(address, b, 2);
        }
        if (address == 0xC002 || address == 0xC003) {
            this.ChrSwap(address, b, 3);
        }
        if (address == 0xD000 || address == 0xD001) {
            this.ChrSwap(address, b, 4);
        }
        if (address == 0xD002 || address == 0xD003) {
            this.ChrSwap(address, b, 5);
        }
        if (address == 0xE000 || address == 0xE001) {
            this.ChrSwap(address, b, 6);
        }
        if (address == 0xE002 || address == 0xE003) {
            this.ChrSwap(address, b, 7);
        }
    }

    @Override
    public byte CHRead(int address) {
        var idx = address / 0x400;
        var offset = address % 0x400;
        return this.getChrom()[this.CHRMapper[idx] * 0x400 + offset];
    }

    /**
     * CHR Select 0 low($B000), high($B001)
     * $B000        $B001
     * 7  bit  0    7  bit  0
     * ---------    ---------
     * .... LLLL    ...H HHHH
     * ||||       | ||||
     * ||||       +-++++- High 5 bits of 1 KiB CHR bank at PPU $0000
     * ++++-------------- Low 4 bits
     * VRC2 only has 4 high bits of CHR select. $B001 bit 4 is ignored.
     * <p>
     * On VRC2a (mapper 22), the low bit is ignored (right shift value by 1).
     * <p>
     * CHR Select 1 low($B002), high($B003)
     * $B002        $B003
     * 7  bit  0    7  bit  0
     * ---------    ---------
     * .... LLLL    ...H HHHH
     * ||||       | ||||
     * ||||       +-++++- High 5 bits of 1 KiB CHR bank at PPU $0400
     * ++++-------------- Low 4 bits
     * VRC2 only has 4 high bits of CHR select. $B003 bit 4 is ignored.
     * <p>
     * On VRC2a (mapper 22), the low bit is ignored (right shift value by 1).
     */
    private void ChrSwap(int address, byte b, int idx) {
        if ((address & 1) == 0) {
            this.chrBank = b & 0x0F;
        } else {
            this.chrBank |= (b & 0x1F) << 4;
            this.CHRMapper[idx] = (this.chrBank & this.chrMode);
        }
    }
}
