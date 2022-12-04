package cn.navclub.nes4j.bin.ppu;

import cn.navclub.nes4j.bin.config.MaskFlag;
import cn.navclub.nes4j.bin.function.CycleDriver;

public class PPURender implements CycleDriver {
    private final PPU ppu;
    //Secondary OAM (holds 8 sprites for the current scanline)
    private final byte[] oam;
    //
    //2 16-bit shift registers - These contain the pattern table data for two tiles.
    // Every 8 cycles, the data for the next tile is loaded into the upper 8 bits of this shift register.
    // Meanwhile, the pixel to render is fetched from one of the lower 8 bits.
    //
    private int shift16;
    private int shift17;
    //
    //2 8-bit shift registers - These contain the palette attributes for the lower 8 pixels
    // of the 16-bit shift register. These registers are fed by a latch which contains the palette attribute
    // for the next tile. Every 8 cycles, the latch is loaded with the palette attribute for the next tile.
    //
    private int shift08;
    private int shift09;

    private long cycles;

    public PPURender(PPU ppu) {
        this.ppu = ppu;
        this.oam = new byte[8];
    }

    @Override
    public void tick() {
        this.cycles++;
        var mask = this.ppu.getMask();
        var sprite = mask.contain(MaskFlag.SHOW_SPRITES);
        var background = mask.contain(MaskFlag.SHOW_BACKGROUND);
        //Background and sprite all hidden
        if (!background && !sprite) {
            return;
        }

    }
}
