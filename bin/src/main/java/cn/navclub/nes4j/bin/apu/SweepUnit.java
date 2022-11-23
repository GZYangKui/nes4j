package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.apu.impl.PulseChannel;
import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 * 滑音单元
 */
public class SweepUnit implements CycleDriver {
    //右移位数
    private int shift;
    //滑音是否开启
    private boolean enable;
    //是否为负
    private boolean negative;
    //分频器
    private final Divider divider;
    //判断至上一次tick以来是否能发生寄存器写操作
    private boolean write;

    public SweepUnit() {
        this.divider = new Divider();
    }

    public void update(byte value) {
        this.write = true;
        this.shift = value & 0x07;
        this.enable = ((value & 0x80) != 0);
        this.negative = (value & 0x08) != 0;
        this.divider.setPeriod((value & 0x70) + 1);
    }

    /**
     * The shifter continuously calculates a result based on the channel's period. The
     * channel's period (from the third and fourth registers) is first shifted right
     * by s bits. If negate is set, the shifted value's bits are inverted, and on the
     * second square channel, the inverted value is incremented by 1. The resulting
     * value is added with the channel's current period, yielding the final result.
     *
     * @param period Current timer period
     * @param index  Current pulse index
     * @return Calculate value
     */
    public int calculate(int period, PulseChannel.PulseIndex index) {
        if (this.shift == 0) {
            return period;
        }
        var result = period >> this.shift;
        if (this.negative) {
            result = ~result;
        }
        if (index == PulseChannel.PulseIndex.PULSE_1) {
            result += 1;
        }
        //
        // When the channel's period is less than 8 or the result of the shifter is
        // greater than $7FF, the channel's DAC receives 0 and the sweep unit doesn't
        // change the channel's period.Otherwise, if the sweep unit is enabled and the
        // shift count is greater than 0, when the divider outputs a clock, the channel's
        // period in the third and fourth registers are updated with the result of the
        // shifter.
        //
        if (!this.enable || result > 0x07ff || period < 8 || !this.divider.output()) {
            return period;
        }
        result = result + period;
        return result;
    }

    /**
     * When the sweep unit is clocked, the divider is *first* clocked and then if
     * there was a write to the sweep register since the last sweep clock, the divider
     * is reset.
     */
    @Override
    public void tick(int cycle) {
        this.divider.tick(cycle);
        if (this.write) {
            this.write = false;
            this.divider.reset();
        }
    }
}
