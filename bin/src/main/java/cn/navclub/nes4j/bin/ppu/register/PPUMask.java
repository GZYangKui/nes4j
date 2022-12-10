package cn.navclub.nes4j.bin.ppu.register;


import cn.navclub.nes4j.bin.config.Register;
import cn.navclub.nes4j.bin.config.MaskFlag;

/**
 * <pre>
 * Mask ($2001) > write
 * Common name: PPUMASK
 * Description: PPU mask register
 * Access: write
 * This register controls the rendering of sprites and backgrounds, as well as colour effects.
 *
 *
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
 * </pre>
 */
public class PPUMask extends Register<MaskFlag> {
    public boolean showSprite() {
        return this.contain(MaskFlag.SHOW_SPRITES);
    }

    public boolean showBackground() {
        return this.contain(MaskFlag.SHOW_BACKGROUND);
    }

    public boolean enableRender() {
        return this.showSprite() || this.showBackground();
    }
}