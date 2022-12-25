package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.apu.impl.PulseChannel;
import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;

/**
 * <p>An NES APU sweep unit can be made to periodically adjust a pulse channel's period up or down.</p>
 * <p>Each <a href="https://www.nesdev.org/wiki/APU_Sweep">sweep unit</a> contains the following:</p>
 * <li>divider</li>
 * <li>reload flag</li>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class SweepUnit implements CycleDriver {
    //Divider
    private final Divider divider;
    private final PulseChannel channel;
    //Shift
    private int shift;
    //Record write operate from the last tick
    private boolean write;
    //Enable flag
    private boolean enable;
    @Getter
    private boolean silence;
    //Negative flag
    private boolean negative;


    public SweepUnit(PulseChannel channel) {
        this.channel = channel;
        this.divider = new Divider();
    }

    /**
     * <pre>
     * A channel's second register configures the sweep unit:
     *
     * eppp nsss       enable, period, negate, shift
     *
     * The divider's period is set to p + 1.
     * </p>
     */
    public void update(byte value) {
        this.write = true;
        this.shift = value & 0x07;
        this.enable = ((value & 0x80) != 0);
        this.negative = (value & 0x08) != 0;
        this.divider.setPeriod(((value & 0x70) >> 4) + 1);
    }

    /**
     * The shifter continuously calculates a result based on the channel's period. The
     * channel's period (from the third and fourth registers) is first shifted right
     * by s bits. If negate is set, the shifted value's bits are inverted, and on the
     * second square channel, the inverted value is incremented by 1. The resulting
     * value is added with the channel's current period, yielding the final result.
     *
     * @param period Current timer period
     * @return Calculate result
     */
    private int calculate(int period) {
        var result = (period >> this.shift);

        if (this.negative) {
            result = -result;
            if (this.channel.isSecond()) {
                result -= 1;
            }
        }

        return result + period;
    }

    /**
     * When the sweep unit is clocked, the divider is *first* clocked and then if
     * there was a write to the sweep register since the last sweep clock, the divider
     * is reset.
     */
    @Override
    public void tick() {
        this.divider.tick();

        if (this.write) {
            this.write = false;
            this.divider.reset();
        }

        var timer = this.channel.timer;
        var result = this.calculate(timer.getPeriod());

        //
        // When the channel's period is less than 8 or the result of the shifter is
        // greater than $7FF, the channel's DAC receives 0 and the sweep unit doesn't
        // change the channel's period.Otherwise, if the sweep unit is enabled and the
        // shift count is greater than 0, when the divider outputs a clock, the channel's
        // period in the third and fourth registers are updated with the result of the
        // shifter.
        //
        this.silence = (timer.period < 8 || result > 0x7ff);
        if (!silence && this.enable && this.shift > 0 && divider.output()) {
            timer.setPeriod(result);
        }
    }
}
