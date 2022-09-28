package cn.navclub.nes4j.bin.core.registers;


import cn.navclub.nes4j.bin.enums.Color;
import cn.navclub.nes4j.bin.enums.MaskFlag;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * PPU mask寄存器
 *
 */
public class MaskRegister {
    private byte bits;

    public MaskRegister() {
        this.bits = 0;
    }

    public boolean grayScala() {
        return this.contain(MaskFlag.GREYSCALE);
    }

    public boolean leftmostSprite() {
        return this.contain(MaskFlag.LEFTMOST_8PXL_SPRITE);
    }

    public boolean leftmostBackground() {
        return this.contain(MaskFlag.LEFTMOST_0PXL_BACKGROUND);
    }

    public boolean showSprites() {
        return this.contain(MaskFlag.SHOW_SPRITES);
    }

    public void update(byte b) {
        this.bits = b;
    }

    /**
     *
     * 获取当前颜色 RGB
     *
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


    private boolean contain(MaskFlag flag) {
        byte b = (byte) (1 << flag.ordinal());
        return (this.bits & b) > 0;
    }
}
