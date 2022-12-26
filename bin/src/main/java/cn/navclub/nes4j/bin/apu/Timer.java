package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.apu.impl.timer.Divider;
import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

/**
 * A timer is used in each of the five channels to control the sound frequency. It contains a divider which
 * is clocked by the CPU clock. The triangle channel's timer is clocked on every CPU cycle, but the pulse, noise,
 * and DMC timers are clocked only on every second CPU cycle and thus produce only even periods.
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 * @see cn.navclub.nes4j.bin.apu.impl.TriangleChannel
 * @see Divider
 */
public class Timer<T extends Sequencer> implements CycleDriver {
    @Getter
    protected int counter;
    @Setter
    @Getter
    protected int period;
    protected final T sequencer;

    public Timer(T sequencer) {
        this.sequencer = sequencer;
    }

    @Override
    public void tick() {
        if (this.counter == 0) {
            this.counter = this.period;
            //Generate sequence
            if (this.sequencer != null) {
                this.sequencer.tick();
            }
        } else {
            if (this.counter > 0)
                this.counter--;
        }
    }
}
