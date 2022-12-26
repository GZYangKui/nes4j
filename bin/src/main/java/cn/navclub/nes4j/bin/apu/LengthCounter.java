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
    @Setter
    private int counter;
    //判断是否停止计数
    @Setter
    private boolean halt;

    /**
     * When clocked by the frame sequencer, if the halt flag is clear and the counter
     * is non-zero, it is decremented.
     */
    @Override
    public void tick() {
        if (this.halt || this.counter == 0) {
            return;
        }
        this.counter--;
    }

    /**
     * <pre>
     * Unless disabled, a write the channel's fourth register immediately reloads the
     * counter with the value from a lookup table, based on the index formed by the
     * upper 5 bits:
     *
     *     iiii i---       length index
     *
     *     bits  bit 3
     *     7-4   0   1
     *         -------
     *     0   $0A $FE
     *     1   $14 $02
     *     2   $28 $04
     *     3   $50 $06
     *     4   $A0 $08
     *     5   $3C $0A
     *     6   $0E $0C
     *     7   $1A $0E
     *     8   $0C $10
     *     9   $18 $12
     *     A   $30 $14
     *     B   $60 $16
     *     C   $C0 $18
     *     D   $48 $1A
     *     E   $10 $1C
     *     F   $20 $1E
     * </pre>
     *
     * @param b Register valuer
     */
    public void lookupTable(byte b) {
        var index = ((b & 0xf0) >> 4) * 2;
        var offset = ((b & 0x08) >> 3) & 0x01;
        this.counter = LOOKUP_TABLE[index + offset];
    }
}
