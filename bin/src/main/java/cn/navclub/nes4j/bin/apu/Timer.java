package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.apu.impl.timer.Divider;
import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

/**
 * Decrement counter implementation
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
        this.counter--;
        if (this.counter <= 0) {
            this.counter = this.period;
            //Generate sequence
            if (this.sequencer != null) {
                this.sequencer.tick();
            }
        }
    }
}
