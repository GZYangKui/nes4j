package cn.navclub.nes4j.bin.util;

import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.enums.MaskFlag;
import cn.navclub.nes4j.bin.enums.NameMirror;
import cn.navclub.nes4j.bin.enums.NameTMirror;

public class PPUUtil {
    /**
     *
     *
     * The NES only has 2 KB to store name tables and attribute tables, allowing it to store two of
     * each. However it can address up to four of each. Mirroring is used to allow it to do this. There
     * are four types of mirroring which are described below, using abbreviations for logical name
     * tables (those that can be addressed), L1 at $2000, L2 at $2400, L3 at $2800 and L4 at
     * $2C00:</p>
     * <li>
     *     Horizontal mirroring maps L1 and L2 to the first physical name table and L3 and L4 to the
     * second as shown in figure 3-4.</p>
     * </li>
     *  <table border="1">
     *      <tr>
     *          <td>Name table 1</td>
     *          <td>Name table 1</td>
     *      </tr>
     *      <tr>
     *          <td>Name table 2</td>
     *          <td>Name table 2</td>
     *      </tr>
     *  </table>
     *  <b>Figure 3-4. Horizontal mirroring.</b>
     *  <li>
     *      Vertical mirroring maps L1 and L3 to the first physical name table and L2 and L4 to the
     * second as shown in figure 3-5.
     *  </li>
     *  <table border="1">
     *      <tr>
     *          <td>Name table 1</td>
     *          <td>Name table 2</td>
     *      </tr>
     *      <tr>
     *          <td>Name table 1</td>
     *          <td>Name table 2</td>
     *      </tr>
     *  </table>
     *  <b>Figure 3-5. Vertical mirroring.</b>
     *
     * @param ppu PPU instance
     * @param t1 First name table
     * @param t2 Second name table
     */
    public static void fillNameTable(PPU ppu, byte[] t1, byte[] t2) {
        var vram = ppu.getVram();
        var ctr = ppu.getControl();
        var nameTable = ctr.nameTableAddr();
        var mirror = ppu.getMirrors();

        if (nameTable == NameTMirror.L1
                || ((mirror == NameMirror.HORIZONTAL && nameTable == NameTMirror.L2)
                || (mirror == NameMirror.VERTICAL && nameTable == NameTMirror.L3))) {
            System.arraycopy(vram, 0, t1, 0, 0x400);
            System.arraycopy(vram, 0x400, t2, 0, 0x400);
        } else if (nameTable == NameTMirror.L4
                || ((mirror == NameMirror.VERTICAL && nameTable == NameTMirror.L2)
                || (mirror == NameMirror.HORIZONTAL && nameTable == NameTMirror.L3))) {
            System.arraycopy(vram, 0, t2, 0, 0x400);
            System.arraycopy(vram, 0x400, t1, 0, 0x400);
        } else {
            throw new RuntimeException("Not satisfy name table fill condition.");
        }
    }

    //
    //
    // Sprite zero hits
    // Sprites are conventionally numbered 0 to 63. Sprite 0 is the sprite controlled by OAM addresses $00-$03, sprite 1 is controlled by $04-$07, ..., and sprite 63 is controlled by $FC-$FF.
    //
    // While the PPU is drawing the picture, when an opaque pixel of sprite 0 overlaps an opaque pixel of the background, this is a sprite zero hit. The PPU detects this condition and sets bit 6 of PPUSTATUS ($2002) to 1 starting at this pixel, letting the CPU know how far along the PPU is in drawing the picture.
    //
    // Sprite 0 hit does not happen:
    //
    // If background or sprite rendering is disabled in PPUMASK ($2001)
    // At x=0 to x=7 if the left-side clipping window is enabled (if bit 2 or bit 1 of PPUMASK is 0).
    // At x=255, for an obscure reason related to the pixel pipeline.
    // At any pixel where the background or sprite pixel is transparent (2-bit color index from the CHR pattern is %00).
    // If sprite 0 hit has already occurred this frame. Bit 6 of PPUSTATUS ($2002) is cleared to 0 at dot 1 of the pre-render line. This means only the first sprite 0 hit in a frame can be detected.
    // Sprite 0 hit happens regardless of the following:
    //
    // Sprite priority. Sprite 0 can still hit the background from behind.
    // The pixel colors. Only the CHR pattern bits are relevant, not the actual rendered colors, and any CHR color index except %00 is considered opaque.
    // The palette. The contents of the palette are irrelevant to sprite 0 hits. For example: a black ($0F) sprite pixel can hit a black ($0F) background as long as neither is the transparent color index %00.
    // The PAL PPU blanking on the left and right edges at x=0, x=1, and x=254 (see Overscan).
    //
    //
    public static boolean checkSpriteZeroHit(PPU ppu) {
        var mask = ppu.getMask();
        //
        // Set when a nonzero pixel of sprite 0 overlaps a nonzero background pixel;
        // Sprite 0 hit does not trigger in any area where the background or sprites are hidden.
        //
        if (!mask.contain(MaskFlag.SHOW_SPRITES) || !mask.contain(MaskFlag.SHOW_BACKGROUND)) {
            return false;
        }

        var oam = ppu.getOam();
        var tx = oam[3] & 0xff;
        var ty = oam[0] & 0xff;
        var idx = oam[1] & 0xff;
        var attr = oam[2] & 0xff;

        var vf = (attr >> 7 & 1) == 1;
        var hf = (attr >> 6 & 1) == 1;
        var ctrl = ppu.getControl();
        var scroll = ppu.getScroll();

        var size = ctrl.spriteSize();
        var bank = size == 0x08 ? ctrl.spritePattern8() : ctrl.spritePattern16(idx);

        if (size == 0x10) {
            idx = idx & 0xfe;
        }
        var tile = new byte[16];
        boolean hit = false;
        for (int k = 8; k <= size; k += 8) {
            System.arraycopy(ppu.getCh(), bank + idx * 16, tile, 0, 16);
            for (int y = 0; y < 8; y++) {
                var l = tile[y] & 0xff;
                var r = tile[y + 8];
                for (int x = 7; x >= 0; x--) {
                    var value = ((r & 0x01) << 1) | l & 0x01;

                    l >>= 1;
                    r >>= 1;

                    //Detect sprite zero opaque pixels
                    if (value == 0) {
                        continue;
                    }
                    final int x0;
                    final int y0;
                    if (!hf && !vf) {
                        x0 = tx + x;
                        y0 = ty + y;
                    } else if (hf && !vf) {
                        x0 = tx + 7 - x;
                        y0 = ty + y;
                    } else if (!hf && vf) {
                        x0 = tx + x;
                        y0 = ty + 7 - y;
                    } else {
                        x0 = tx + 7 - x;
                        y0 = ty + 7 - y;
                    }
                    //Check x0 and y0 position background

                }
            }
            ty += 8;
        }
        return hit;
    }
}
