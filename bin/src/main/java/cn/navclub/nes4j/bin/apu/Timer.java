package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

/**
 * Decrement counter implementation
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 * @see cn.navclub.nes4j.bin.apu.impl.TriangleChannel.TTimer
 * @see Divider
 */
public class Timer implements CycleDriver {
    @Getter
    protected int counter;
    @Setter
    @Getter
    protected int period;
    protected final Sequencer sequencer;

    public Timer(Sequencer sequencer) {
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
