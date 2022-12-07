package cn.navclub.nes4j.bin.ppu.register;

import cn.navclub.nes4j.bin.config.PStatus;
import cn.navclub.nes4j.bin.config.Register;

/**
 * Status ($2002) < read
 * Common name: PPUSTATUS
 * Description: PPU status register
 * Access: read
 * This register reflects the state of various functions inside the PPU.
 * It is often used for determining timing.To determine when the PPU has reached a given pixel of the screen,
 * put an opaque (non-transparent) pixel of sprite 0 there.
 *
 * ---- ----
 * VSO. ....
 * |||| ||||
 * |||+-++++- PPU open bus. Returns stale PPU bus contents.
 * ||+------- Sprite overflow. The intent was for this flag to be set
 * ||         whenever more than eight sprites appear on a scanline, but a
 * ||         hardware bug causes the actual behavior to be more complicated
 * ||         and generate false positives as well as false negatives; see
 * ||         PPU sprite evaluation. This flag is set during sprite
 * ||         evaluation and cleared at dot 1 (the second dot) of the
 * ||         pre-render line.
 * |+-------- Sprite 0 Hit.  Set when a nonzero pixel of sprite 0 overlaps
 * |          a nonzero background pixel; cleared at dot 1 of the pre-render
 * |          line.  Used for raster timing.
 * +--------- Vertical blank has started (0: not in vblank; 1: in vblank).
 *            Set at dot 1 of line 241 (the line *after* the post-render
 *            line); cleared after reading $2002 and at dot 1 of the
 *            pre-render line.
 */
public class PPUStatus extends Register<PStatus> {
}
