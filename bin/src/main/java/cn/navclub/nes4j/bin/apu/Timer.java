package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

/**
 * 归零计数器实现
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
        if (this.counter == 0) {
            this.counter = this.period;
            //Generate sequence
            if (this.sequencer != null) {
                this.sequencer.tick();
            }
        } else {
            this.counter--;

        }
    }
}
