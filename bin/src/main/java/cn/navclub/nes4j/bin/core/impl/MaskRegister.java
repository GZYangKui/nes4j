package cn.navclub.nes4j.bin.core.impl;


import cn.navclub.nes4j.bin.core.SRegister;
import cn.navclub.nes4j.bin.enums.Color;
import cn.navclub.nes4j.bin.enums.MaskFlag;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * PPU mask寄存器
 *
 */
public class MaskRegister extends SRegister {
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
}
