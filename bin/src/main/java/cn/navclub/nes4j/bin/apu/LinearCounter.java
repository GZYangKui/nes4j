package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

public class LinearCounter implements CycleDriver {
    @Getter
    private int counter;
    //停止标识
    @Setter
    private boolean halt;
    @Getter
    private boolean control;
    private int defaultValue;

    /**
     *
     * Register $4008 contains a control flag and reload value:
     *
     *     crrr rrrr   control flag, reload value
     *
     * Note that the bit position for the control flag is also mapped to a flag in the
     * Length Counter.
     *
     */
    public void update(byte b) {
        this.defaultValue = (b & 0x7f);
        this.control = (b & 0x80) != 0;
    }

    /**
     *
     * When clocked by the frame sequencer, the following actions occur in order:
     *
     *     1) If halt flag is set, set counter to reload value, otherwise if counter
     * is non-zero, decrement it.
     *
     *     2) If control flag is clear, clear halt flag.
     *
     *
     */
    @Override
    public void tick(int cycle) {
        if (this.halt) {
            this.counter = this.defaultValue;
        } else {
            if (this.counter != 0) {
                this.counter--;
            }
        }
        if (!this.control) {
            this.halt = false;
        }
    }
}
