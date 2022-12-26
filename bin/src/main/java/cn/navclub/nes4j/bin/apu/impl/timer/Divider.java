package cn.navclub.nes4j.bin.apu.impl.timer;

import cn.navclub.nes4j.bin.apu.Sequencer;
import cn.navclub.nes4j.bin.apu.Timer;
import lombok.Setter;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <li>
 * A divider outputs a clock periodically. It contains a period reload value, P, and a counter,
 * that starts at P. When the divider is clocked, if the counter is currently 0, it is reloaded with
 * P and generates an output clock, otherwise the counter is decremented. In other words, the divider's
 * period is P + 1.
 * </li>
 * <li>
 * A divider can also be forced to reload its counter immediately (counter = P), but this does not output
 * a clock. Similarly, changing a divider's period reload value does not affect the counter. Some counters
 * offer no way to force a reload, but setting P to 0 at least synchronizes it to a known state once the
 * current count expires.
 * </li>
 * <li>
 * A divider may be implemented as a down counter (5, 4, 3, ...) or as a linear feedback shift register
 * (LFSR). The dividers in the pulse and triangle channels are linear down-counters. The dividers for
 * noise, DMC, and the APU Frame Counter are implemented as LFSRs to save gates compared to the equivalent
 * down counter.
 * </li>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class Divider extends Timer<Sequencer> {
    private final Consumer<Void> consumer;

    public Divider(Consumer<Void> consumer) {
        super(null);
        this.consumer = consumer;
    }

    @Override
    public void tick() {
        this.counter--;
        //Reset divider and clock
        if (this.counter == 0) {
            this.reset();
            if (this.consumer != null) {
                consumer.accept(null);
            }
        }
    }

    public void reset() {
        this.counter = this.period;
    }
}
