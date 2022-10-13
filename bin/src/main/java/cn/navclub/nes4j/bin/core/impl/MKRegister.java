package cn.navclub.nes4j.bin.core.impl;


import cn.navclub.nes4j.bin.core.SRegister;
import cn.navclub.nes4j.bin.enums.Color;
import cn.navclub.nes4j.bin.enums.MaskFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * Mask ($2001) > write
 * Common name: PPUMASK
 * Description: PPU mask register
 * Access: write
 * This register controls the rendering of sprites and backgrounds, as well as colour effects.
 * <p>
 * 7  bit  0
 * ---- ----
 * BGRs bMmG
 * |||| ||||
 * |||| |||+- Greyscale (0: normal color, 1: produce a greyscale display)
 * |||| ||+-- 1: Show background in leftmost 8 pixels of screen, 0: Hide
 * |||| |+--- 1: Show sprites in leftmost 8 pixels of screen, 0: Hide
 * |||| +---- 1: Show background
 * |||+------ 1: Show sprites
 * ||+------- Emphasize red (green on PAL/Dendy)
 * |+-------- Emphasize green (red on PAL/Dendy)
 * +--------- Emphasize blue
 */
public class MKRegister extends SRegister {
    /**
     * 获取当前颜色 RGB
     */
    public List<Color> emphasise() {
        var colors = new ArrayList<Color>();

        if (this.contain(MaskFlag.EMPHASISE_RED)) {
            colors.add(Color.RED);
        }

        if (this.contain(MaskFlag.EMPHASISE_GREEN)) {
            colors.add(Color.GREEN);
        }

        if (this.contain(MaskFlag.EMPHASISE_BLUE)) {
            colors.add(Color.BLUE);
        }

        return colors;
    }
}
