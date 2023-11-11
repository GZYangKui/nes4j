package cn.navclub.nes4j.bin.io;


import cn.navclub.nes4j.bin.config.NESFormat;
import cn.navclub.nes4j.bin.config.NMapper;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.config.TV;
import cn.navclub.nes4j.bin.util.BinUtil;
import cn.navclub.nes4j.bin.util.IOUtil;

import lombok.Getter;

import java.io.File;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;
import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * <pre>
 * iNES file format
 * An iNES file consists of the following sections, in order:
 *
 * Header (16 bytes)
 * Trainer, if present (0 or 512 bytes)
 * PRG ROM data (16384 * x bytes)
 * CHR ROM data, if present (8192 * y bytes)
 * PlayChoice INST-ROM, if present (0 or 8192 bytes)
 * PlayChoice PROM, if present (16 bytes Data, 16 bytes CounterOut) (this is often missing, see PC10 ROM-Images for details)
 * Some ROM-Images additionally contain a 128-byte (or sometimes 127-byte) title at the end of the file.
 *
 * The format of the header is as follows:
 *
 * 0-3: Constant $4E $45 $53 $1A ("NES" followed by MS-DOS end-of-file)
 * 4: Size of PRG ROM in 16 KB units
 * 5: Size of CHR ROM in 8 KB units (Value 0 means the board uses CHR RAM)
 * 6: Flags 6 - Mapper, mirroring, battery, trainer
 * 7: Flags 7 - Mapper, VS/Playchoice, NES 2.0
 * 8: Flags 8 - PRG-RAM size (rarely used extension)
 * 9: Flags 9 - TV system (rarely used extension)
 * 10: Flags 10 - TV system, PRG-RAM presence (unofficial, rarely used extension)
 * 11-15: Unused padding (should be filled with zero, but some rippers put their name across bytes 7-15)
 * Flags 6
 * 76543210
 * ||||||||
 * |||||||+- Mirroring: 0: horizontal (vertical arrangement) (CIRAM A10 = PPU A11)
 * |||||||              1: vertical (horizontal arrangement) (CIRAM A10 = PPU A10)
 * ||||||+-- 1: Cartridge contains battery-backed PRG RAM ($6000-7FFF) or other persistent memory
 * |||||+--- 1: 512-byte trainer at $7000-$71FF (stored before PRG data)
 * ||||+---- 1: Ignore mirroring control or above mirroring bit; instead provide four-screen VRAM
 * ++++----- Lower nybble of mapper number
 * In the iNES format, cartridge boards are divided into classes called "mappers" based on similar board hardware and behavior, and each mapper has an 8-bit number (or 12-bit in NES 2.0). The low 4-bits of this mapper are given here in bits 4-7 of this field.
 *
 * The presence of persistent saved memory is given by bit 1. This usually takes the form of battery-backed PRG-RAM at $6000, but there are some mapper-specific exceptions:
 *
 * UNROM 512 and GTROM use flash memory to store their game state by rewriting the PRG-ROM area.
 * Nametable Mirroring
 * See: Nametable Mirroring
 *
 * For mappers with hard-wired mirroring, connecting CIRAM A10 to PPU A10 or A11 for a vertical or horizontal arrangement is specified by bit 0.
 *
 * Some mappers have a 4-screen variation of the board, which is specified with bit 3:
 *
 * MMC3 for Rad Racer 2
 * Mapper 206 for Gauntlet
 * A few mappers override normal usage of the nametable mirroring bits:
 *
 * UNROM 512 uses %....1..0 to indicate a 1-screen board, and %....1..1 to indicate a 4-screen board.
 * Mapper 218 (Magic Floor) has 4 unusual CIRAM configurations corresponding to each of the possible values.
 * Ambiguity:
 *
 * Many mappers (MMC1, MMC3, AxROM...) have mapper controlled nametable mirroring. These will ignore bit 0.
 * Several mappers have some form of 4-screen as their only option. ROMs might be found with bit 3 set to redundantly indicate this:
 * Napoleon Senki
 * Vs. System
 * GTROM
 * Mappers that share 4-screen nametable RAM with CHR-RAM may interact with the NES 2.0 CHR-RAM in byte 11.
 * Mapper 70 had a 1-screen variant that was sometimes specified with bit 3 set. This was relocated to Mapper 152.
 * Theoretically bit 3 could be used for most mappers that had hard-wired mirroring to transparently provide 4KB of VRAM for 4-screen instead. However, emulator support for this is largely untested for mappers without prior 4-screen variations.
 * Trainer
 * The trainer usually contains mapper register translation and CHR-RAM caching code for
 *
 * early RAM cartridges that could not mimic mapper ASICs and only had 32 KiB of CHR-RAM;
 * Nesticle, an old but influential NES emulator for DOS.
 * It is not used on unmodified dumps of original ROM cartridges.
 *
 * Flags 7
 * 76543210
 * ||||||||
 * |||||||+- VS Unisystem
 * ||||||+-- PlayChoice-10 (8KB of Hint Screen data stored after CHR data)
 * ||||++--- If equal to 2, flags 8-15 are in NES 2.0 format
 * ++++----- Upper nybble of mapper number
 * The PlayChoice-10 bit is not part of the official specification, and most emulators simply ignore the extra 8KB of data. PlayChoice games are designed to look good with the 2C03 RGB PPU, which handles color emphasis differently from a standard NES PPU.
 *
 * Vs. games have a coin slot and different palettes. The detection of which palette a particular game uses is left unspecified.
 *
 * NES 2.0 is a more recent extension to the format that allows more flexibility in ROM and RAM size, among other things.
 *
 * Flags 8
 * 76543210
 * ||||||||
 * ++++++++- PRG RAM size
 * Size of PRG RAM in 8 KB units (Value 0 infers 8 KB for compatibility; see PRG RAM circuit)
 *
 * This was a later extension to the iNES format and not widely used. NES 2.0 is recommended for specifying PRG RAM size instead.
 *
 * Flags 9
 * 76543210
 * ||||||||
 * |||||||+- TV system (0: NTSC; 1: PAL)
 * +++++++-- Reserved, set to zero
 * Though in the official specification, very few emulators honor this bit as virtually no ROM images in circulation make use of it.
 *
 * Flags 10
 * 76543210
 *   ||  ||
 *   ||  ++- TV system (0: NTSC; 2: PAL; 1/3: dual compatible)
 *   |+----- PRG RAM ($6000-$7FFF) (0: present; 1: not present)
 *   +------ 0: Board has no bus conflicts; 1: Board has bus conflicts
 * This byte is not part of the official specification, and relatively few emulators honor it.
 *
 * The PRG RAM Size value (stored in byte 8) was recently added to the official specification; as such, virtually no ROM images in circulation make use of it.
 *
 * Older versions of the iNES emulator ignored bytes 7-15, and several ROM management tools wrote messages in there. Commonly, these will be filled with "DiskDude!", which results in 64 being added to the mapper number.
 *
 * A general rule of thumb: if the last 4 bytes are not all zero, and the header is not marked for NES 2.0 format, an emulator should either mask off the upper 4 bits of the mapper number or simply refuse to load the ROM.
 *
 * Variant comparison
 * Over the years, the header of the .NES file format has changed as new features became needed. There are three discernable generations:
 *
 * Archaic iNES
 * Created by Marat Fayzullin and used in very old versions of iNES and in NESticle. ROM image conversion and auditing tools tended to store signature strings at offsets 7-15.
 * iNES 0.7
 * Created by Marat Fayzullin when the scene discovered the diversity of NES cartridge hardware. Mapper high nibble is supported in emulators since roughly 2000.
 * iNES
 * Later revisions added byte 8 (PRG RAM size) and byte 9 (TV system), though few other emulators supported these fields.
 * NES 2.0
 * Created by kevtris for the FPGA Kevtendo and maintained by the NESdev community to clarify ambiguous cases that previous headers did not clarify. Became widely adopted starting in the 2010s.
 * Thing	Archaic iNES	iNES	NES 2.0
 * Byte 7	Unused	Mapper high nibble, Vs.	Mapper high nibble, NES 2.0 signature, PlayChoice, Vs.
 * Byte 8	Unused	Total PRG RAM size (linear)	Mapper highest nibble, mapper variant
 * Byte 9	Unused	TV system	Upper bits of ROM size
 * Byte 10	Unused	Unused	PRG RAM size (logarithmic; battery and non-battery)
 * Byte 11	Unused	Unused	VRAM size (logarithmic; battery and non-battery)
 * Byte 12	Unused	Unused	TV system
 * Byte 13	Unused	Unused	Vs. PPU variant
 * Byte 14	Unused	Unused	Miscellaneous ROMs
 * Byte 15	Unused	Unused	Default expansion device
 * Mappers supported	0-15	0-255	0-4095
 * Recommended detection procedure:
 *
 * If byte 7 AND $0C = $08, and the size taking into account byte 9 does not exceed the actual size of the ROM image, then NES 2.0.
 * If byte 7 AND $0C = $04, archaic iNES.
 * If byte 7 AND $0C = $00, and bytes 12-15 are all 0, then iNES.
 * Otherwise, iNES 0.7 or archaic iNES.
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@Getter
public class Cartridge {
    private final static int HEADER_SIZE = 16;
    private final TV tv;
    private final int chSize;
    private final int rgbSize;
    private final byte[] chrom;
    private final byte[] rgbrom;
    private final byte[] train;
    private final NMapper mapper;
    private final NESFormat format;
    private final byte[] cellaneous;
    private final NameMirror mirrors;

    public Cartridge(byte[] buffer) {
        var headers = new byte[HEADER_SIZE];
        //从原始数据中复制Header数据
        System.arraycopy(buffer, 0, headers, 0, HEADER_SIZE);

        this.format = this.parseFormat(headers);

        this.chSize = this.calChSize(headers);
        var rgbSize = this.calRgbSize(headers);

        var flag6 = uint8(headers[6]);
        var flag7 = uint8(headers[7]);
        var flag8 = uint8(headers[8]);
        var flag9 = uint8(headers[9]);

        var mapper = (flag7 & 0xf0) | ((flag6 & 0xf0) >> 4);

        //NES2.0包含12位
        if (this.format == NESFormat.NES_20) {
            mapper |= ((flag8 & 0b0000_1111) << 8);
        }

        if (mapper >= NMapper.values().length) {
            this.mapper = NMapper.UNKNOWN;
        } else {
            this.mapper = NMapper.values()[mapper];
        }

        NameMirror mirrors;
        if (((flag6 >> 3) & 0x01) == 1) {
            mirrors = NameMirror.FOUR_SCREEN;
        } else {
            mirrors = NameMirror.values()[flag6 & 1];
        }

        //UNROM 512 uses %....1..0 to indicate a 1-screen board, and %....1..1 to indicate a 4-screen board.
        if (this.mapper == NMapper.UX_ROM && mirrors == NameMirror.FOUR_SCREEN && (flag6 & 0x01) == 0) {
            mirrors = NameMirror.ONE_SCREEN_LOWER;
        }

        this.mirrors = mirrors;
        this.tv = TV.values()[flag9 & 0x01];

        var trainSize = this.trainAreaSize(flag6);

        train = new byte[trainSize];
        chrom = new byte[Math.max(chSize, 8192)];
        rgbrom = new byte[Math.max(rgbSize, 32 * 1024)];

        if (trainSize > 0) {
            System.arraycopy(buffer, HEADER_SIZE, train, 0, trainSize);
        }
        var offset = HEADER_SIZE + trainSize;
        System.arraycopy(buffer, offset, rgbrom, 0, rgbSize);

        //When rpg-rom size less than 32kb copy first part fill second part
        if (rgbSize == 0x4000) {
            System.arraycopy(rgbrom, 0, rgbrom, rgbSize, rgbSize);
        }

        if (chSize > 0) {
            offset += rgbSize;
            System.arraycopy(buffer, offset, chrom, 0, chSize);
        }

        var left = buffer.length - chSize - rgbSize - trainSize - HEADER_SIZE;
        if (this.format == NESFormat.NES_20 && left > 0) {
            cellaneous = new byte[left];
            offset += chSize;
            System.arraycopy(buffer, offset, this.cellaneous, 0, left);
        } else {
            this.cellaneous = new byte[0];
        }
        this.rgbSize = this.rgbrom.length;
    }

//    public Cartridge(NameMirror mirror, byte[] chrom, byte[] rgbrom) {
//        this.tv = TV.NTSC;
//        this.chrom = chrom;
//        this.mirrors = mirror;
//        this.rgbrom = rgbrom;
//        this.train = new byte[0];
//        this.mapper = NMapper.NROM;
//        this.format = NESFormat.INES;
//        this.cellaneous = new byte[0];
//        this.chSize = this.chrom.length;
//        this.rgbSize = this.rgbrom.length;
//    }

    public Cartridge(File file) {
        this(IOUtil.readFileAllByte(file));
    }

    private int calChSize(byte[] headers) {
        var lsb = headers[5];
        var size = lsb & 0xff;
        var scale = 8 * 1024;
        if (this.format == NESFormat.NES_20) {
            var msb = (headers[9]) >>> 4;
            size = (lsb | msb << 8);
            if (msb > 0x0e) {
                scale = 0;
            }
        }
        return size * scale;
    }

    /**
     * 计算RGB-ROM大小
     */
    private int calRgbSize(byte[] headers) {
        var lsb = headers[4];
        var size = (lsb & 0xff);
        var scale = 16 * 1024;
        if (this.format == NESFormat.NES_20) {
            var msb = headers[9] & 0x0f;
            size = BinUtil.toInt(new byte[]{lsb, int8(msb), 0, 0});
            if (msb > 0x0e) {
                scale = 0;
            }
        }
        return size * scale;
    }

    /**
     * 判断当前文件是INES格式还是NES_2.0格式
     */
    private NESFormat parseFormat(byte[] headers) {
        if (!(headers[0] == 'N' && headers[1] == 'E' && headers[2] == 'S' && headers[3] == 0x1a)) {
            throw new RuntimeException("Only support ines and nes_2.0 binary format.");
        }
        final NESFormat nesFormat;
        if ((headers[7] & 0x0c) == 0x08) {
            nesFormat = NESFormat.NES_20;
        } else {
            nesFormat = NESFormat.INES;
        }
        return nesFormat;
    }

    /**
     * 判断是否存在Trainer area
     */
    public int trainAreaSize(int flag6) {
        var has = (flag6 & 0b0000_0100) > 0;
        return has ? 512 : 0;
    }

}
