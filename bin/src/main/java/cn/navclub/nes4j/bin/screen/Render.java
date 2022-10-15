package cn.navclub.nes4j.bin.screen;

import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.util.PatternTableUtil;

public class Render {
    public void render(PPU ppu, Frame frame) {
        var bank = ppu.getControl().BPatternTable();
        for (int i = 0; i < 0x3c0; i++) {
            var index = ppu.getVram()[i];
            var x = i % 32;
            var y = i / 32;
            var arr = new byte[16];
            var offset = bank + index * 16;
            System.arraycopy(ppu.getCh(), offset, arr, 0, 16);
            var pixles = PatternTableUtil.tiles(arr);
        }
    }
}
