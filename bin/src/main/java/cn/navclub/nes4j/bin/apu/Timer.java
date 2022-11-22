package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * 归零计数器实现
 *
 */
public class Timer implements CycleDriver {
    @Getter
    private int counter;
    @Setter
    @Getter
    private int period;
    private final Sequencer sequencer;

    public Timer(Sequencer sequencer) {
        this.sequencer = sequencer;
    }

    @Override
    public void tick(int cycle) {
        if (this.counter == 0) {
            this.counter = this.period;
        } else {
            this.counter--;
            //生成序列
            if (this.counter == 0 && this.sequencer != null) {
                this.sequencer.tick(cycle);
            }
        }
    }
}
