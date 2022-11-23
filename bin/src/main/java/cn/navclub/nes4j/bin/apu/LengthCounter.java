package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

public class LengthCounter implements CycleDriver {
    private static final int[] LOOKUP_TABLE = {
            0x0a, 0xfe,
            0x14, 0x02,
            0x28, 0x04,
            0x50, 0x06,
            0xa0, 0x08,
            0x3c, 0x0a,
            0x0e, 0x0c,
            0x1a, 0x0e,
            0x0c, 0x10,
            0x18, 0x12,
            0x30, 0x14,
            0x60, 0x16,
            0xc0, 0x18,
            0x48, 0x1a,
            0x10, 0x1c,
            0x20, 0x1e
    };
    @Getter
    private int counter;
    //判断是否停止计数
    @Setter
    private boolean halt;
    //判断是否禁用
    private boolean disable;

    @Override
    public void tick(int cycle) {
        //
        // When clocked by the frame sequencer, if the halt flag is clear and the counter
        // is non-zero, it is decremented.
        //
        if (this.halt || this.counter == 0 || this.disable) {
            return;
        }
        this.counter--;
    }

    public void lookupTable(byte b) {
        if (this.disable) {
            return;
        }
        var msb = (b & 0xf0) >> 4;
        var index = msb * 2;
        var offset = (b & 0x08) >> 3;
        this.counter = LOOKUP_TABLE[index + offset];
    }

    public void setDisable(boolean disable) {
        if (disable) {
            this.counter = 0;
        }
        this.disable = disable;
    }
}
