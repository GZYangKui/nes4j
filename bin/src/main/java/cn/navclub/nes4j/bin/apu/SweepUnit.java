package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.apu.impl.Pulse;
import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 * 滑音单元
 */
public class SweepUnit implements CycleDriver {
    //右移位数
    int shift;
    //是否为负
    boolean negative;
    //滑音是否开启
    boolean enable;
    //方形波
    final Pulse pulse;
    //分频器
    final Divider divider;

    public SweepUnit(Pulse pulse) {
        this.pulse = pulse;
        this.divider = new Divider();
    }

    public void update(byte value) {
        this.shift = value & 0x07;
        this.enable = ((value & 0x80) != 0);
        this.negative = (value & 0x08) != 0;

        this.divider.setPeriod((value & 0x70) + 1);

        if (this.shift <= 0) {
            return;
        }

        //
        // 调节方形波周期
        //
        var timer = this.pulse.getTimer();
        var period = divider.getPeriod();
        var result = period << this.shift;
        if (this.negative) {
            result = ~result;
        }
        if (this.pulse.getIndex() == Pulse.PulseIndex.PULSE_1) {
            result += 1;
        }
        //
        // When the channel's period is less than 8 or the result of the shifter is
        // greater than $7FF, the channel's DAC receives 0 and the sweep unit doesn't
        // change the channel's period.
        //
        if (!this.enable || result > 0x07ff || period < 8) {
            return;
        }
        result = result + period;
        //更新通道周期
        divider.setPeriod(result);
    }


    @Override
    public void tick(int cycle) {

    }
}
