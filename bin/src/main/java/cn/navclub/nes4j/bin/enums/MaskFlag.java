package cn.navclub.nes4j.bin.enums;

// 7  bit  0
// ---- ----
// BGRs bMmG
// |||| ||||
// |||| |||+- Greyscale (0: normal color, 1: produce a greyscale display)
// |||| ||+-- 1: Show background in leftmost 8 pixels of screen, 0: Hide
// |||| |+--- 1: Show sprites in leftmost 8 pixels of screen, 0: Hide
// |||| +---- 1: Show background
// |||+------ 1: Show sprites
// ||+------- Emphasize red
// |+-------- Emphasize green
// +--------- Emphasize blue

public enum MaskFlag {
    GREYSCALE,
    LEFTMOST_0PXL_BACKGROUND,
    LEFTMOST_8PXL_SPRITE,
    SHOW_BACKGROUND,
    SHOW_SPRITES,
    EMPHASISE_RED,
    EMPHASISE_GREEN,
    EMPHASISE_BLUE
}
