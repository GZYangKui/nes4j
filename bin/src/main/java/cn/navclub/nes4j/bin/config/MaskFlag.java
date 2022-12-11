package cn.navclub.nes4j.bin.config;

/**
 *
 *
 *Mask ($2001) > write
 *
 * Common name: PPUMASK
 * Description: PPU mask register
 * Access: write
 * This register controls the rendering of sprites and backgrounds, as well as colour effects.
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
 *
 */

public enum MaskFlag {
    // Greyscale (0: normal color, 1: produce a greyscale display)
    GREYSCALE,
    //
    // Bits 1 and 2 enable rendering of the background and sprites in the leftmost 8 pixel columns.
    // Setting these bits to 0 will mask these columns, which is often useful in horizontal scrolling
    // situations where you want partial sprites or tiles to scroll in from the left.
    //
    LEFTMOST_8PXL_BACKGROUND,
    LEFTMOST_8PXL_SPRITE,
    //Bits 3 and 4 enable the rendering of background and sprites, respectively.
    SHOW_BACKGROUND,
    SHOW_SPRITES,
    //
    // Bit 0 controls a greyscale mode, which causes the palette to use only the colors from the grey
    // column: $00, $10, $20, $30. This is implemented as a bitwise AND with $30 on any value read from
    // PPU $3F00-$3FFF, both on the display and through PPUDATA. Writes to the palette through PPUDATA are
    // not affected. Also note that black colours like $0F will be replaced by a non-black grey $00.
    // Bits 5, 6 and 7 control a color "emphasis" or "tint" effect. See Colour emphasis for details.
    // Note that the emphasis bits are applied independently of bit 0, so they will still tint the color of the grey image.
    //
    EMPHASISE_RED,
    EMPHASISE_GREEN,
    EMPHASISE_BLUE
}
