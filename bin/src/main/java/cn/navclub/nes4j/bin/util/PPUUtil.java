//package cn.navclub.nes4j.bin.util;
//
//import cn.navclub.nes4j.bin.ppu.PPU;
//import cn.navclub.nes4j.bin.config.NameMirror;
//import cn.navclub.nes4j.bin.config.NameTMirror;
//
//public class PPUUtil {
//    /**
//     *
//     *
//     * The NES only has 2 KB to store name tables and attribute tables, allowing it to store two of
//     * each. However it can address up to four of each. Mirroring is used to allow it to do this. There
//     * are four types of mirroring which are described below, using abbreviations for logical name
//     * tables (those that can be addressed), L1 at $2000, L2 at $2400, L3 at $2800 and L4 at
//     * $2C00:</p>
//     * <li>
//     *     Horizontal mirroring maps L1 and L2 to the first physical name table and L3 and L4 to the
//     * second as shown in figure 3-4.</p>
//     * </li>
//     *  <table border="1">
//     *      <tr>
//     *          <td>Name table 1</td>
//     *          <td>Name table 1</td>
//     *      </tr>
//     *      <tr>
//     *          <td>Name table 2</td>
//     *          <td>Name table 2</td>
//     *      </tr>
//     *  </table>
//     *  <b>Figure 3-4. Horizontal mirroring.</b>
//     *  <li>
//     *      Vertical mirroring maps L1 and L3 to the first physical name table and L2 and L4 to the
//     * second as shown in figure 3-5.
//     *  </li>
//     *  <table border="1">
//     *      <tr>
//     *          <td>Name table 1</td>
//     *          <td>Name table 2</td>
//     *      </tr>
//     *      <tr>
//     *          <td>Name table 1</td>
//     *          <td>Name table 2</td>
//     *      </tr>
//     *  </table>
//     *  <b>Figure 3-5. Vertical mirroring.</b>
//     *
//     * @param ppu PPU instance
//     * @param t1 First name table
//     * @param t2 Second name table
//     */
//    public static void fillNameTable(PPU ppu, byte[] t1, byte[] t2) {
//        var vram = ppu.getVram();
//        var ctr = ppu.getCtr();
//        var nameTable = ctr.nameTableAddr();
//        var mirror = ppu.getMirrors();
//
//        if (nameTable == NameTMirror.L1
//                || ((mirror == NameMirror.HORIZONTAL && nameTable == NameTMirror.L2)
//                || (mirror == NameMirror.VERTICAL && nameTable == NameTMirror.L3))) {
//            System.arraycopy(vram, 0, t1, 0, 0x400);
//            System.arraycopy(vram, 0x400, t2, 0, 0x400);
//        } else if (nameTable == NameTMirror.L4
//                || ((mirror == NameMirror.VERTICAL && nameTable == NameTMirror.L2)
//                || (mirror == NameMirror.HORIZONTAL && nameTable == NameTMirror.L3))) {
//            System.arraycopy(vram, 0, t2, 0, 0x400);
//            System.arraycopy(vram, 0x400, t1, 0, 0x400);
//        } else {
//            throw new RuntimeException("Not satisfy name table fill condition.");
//        }
//    }
//
//    /**
//     *
//     *
//     * <p>
//     *     The attribute table is a 64-byte array at the end of each nametable that controls which palette is assigned to each part of the background.
//     * Each attribute table, starting at $23C0, $27C0, $2BC0, or $2FC0, is arranged as an 8x8 byte array:
//     * </p>
//     *
//     *<pre>
//     *        2xx0    2xx1    2xx2    2xx3    2xx4    2xx5    2xx6    2xx7
//     *      ,-------+-------+-------+-------+-------+-------+-------+-------.
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     * 2xC0:| - + - | - + - | - + - | - + - | - + - | - + - | - + - | - + - |
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      +-------+-------+-------+-------+-------+-------+-------+-------+
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     * 2xC8:| - + - | - + - | - + - | - + - | - + - | - + - | - + - | - + - |
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      +-------+-------+-------+-------+-------+-------+-------+-------+
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     * 2xD0:| - + - | - + - | - + - | - + - | - + - | - + - | - + - | - + - |
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      +-------+-------+-------+-------+-------+-------+-------+-------+
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     * 2xD8:| - + - | - + - | - + - | - + - | - + - | - + - | - + - | - + - |
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      +-------+-------+-------+-------+-------+-------+-------+-------+
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     * 2xE0:| - + - | - + - | - + - | - + - | - + - | - + - | - + - | - + - |
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      +-------+-------+-------+-------+-------+-------+-------+-------+
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     * 2xE8:| - + - | - + - | - + - | - + - | - + - | - + - | - + - | - + - |
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      +-------+-------+-------+-------+-------+-------+-------+-------+
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     * 2xF0:| - + - | - + - | - + - | - + - | - + - | - + - | - + - | - + - |
//     *      |   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      +-------+-------+-------+-------+-------+-------+-------+-------+
//     * 2xF8:|   .   |   .   |   .   |   .   |   .   |   .   |   .   |   .   |
//     *      `-------+-------+-------+-------+-------+-------+-------+-------'
//     *</pre>
//     *
//     * <p>
//     *  Each byte controls the palette of a 32×32 pixel or 4×4 tile part of the nametable and is divided
//     *into four 2-bit areas. Each area covers 16×16 pixels or 2×2 tiles, the size of a [?] block in Super Mario Bros.
//     *Given palette numbers topleft, topright, bottomleft, bottomright, each in the range 0 to 3, the value of the byte is
//     * </p>
//     *
//     * <b>
//     *     value = (bottomright << 6) | (bottomleft << 4) | (topright << 2) | (topleft << 0)
//     * </b>
//     *
//     * <p>
//     *     <a href="https://www.nesdev.org/wiki/PPU_attribute_tables">More detail for nametable render</a>
//     * </p>
//     *
//     * @return A byte array contain one backdrop color and four three-color subpalettes.
//     */
//    public static byte[] bgPalette(PPU ppu, byte[] atrTable, int column, int row) {
//        var idx = 0;
//        var test = 3;
//        var a = column % 4 / 2;
//        var b = row % 4 / 2;
//        var attrByte = atrTable[row / 4 * 8 + column / 4] & 0xff;
//        if (a == 0 && b == 0)
//            idx = attrByte & test;
//        else if (a == 1 && b == 0)
//            idx = attrByte >> 2 & test;
//        else if (a == 0 && b == 1)
//            idx = attrByte >> 4 & test;
//        else if (a == 1 && b == 1)
//            idx = attrByte >> 6 & test;
//
//        var offset = 1 + idx * 4;
//
//        return new byte[]{
//                ppu.getPaletteTable()[0],
//                ppu.getPaletteTable()[offset],
//                ppu.getPaletteTable()[offset + 1],
//                ppu.getPaletteTable()[offset + 2]
//        };
//    }
//}
