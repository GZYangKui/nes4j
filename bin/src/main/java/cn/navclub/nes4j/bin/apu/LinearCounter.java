package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

public class LinearCounter implements CycleDriver {
    @Getter
    private int counter;
    //Reload flag
    @Setter
    private boolean halt;
    @Getter
    private boolean control;
    private int reloadValue;

    /**
     * <pre>
     * Register $4008 contains a control flag and reload value:
     *
     *     crrr rrrr   control flag, reload value
     *
     * Note that the bit position for the control flag is also mapped to a flag in the
     * Length Counter.
     * </pre>
     */
    public void update(byte b) {
        this.reloadValue = (b & 0x7f);
        this.control = (b & 0x80) == 0x80;
    }

    /**
     * When clocked by the frame sequencer, the following actions occur in order:
     *
     * <li> If halt flag is set, set counter to reload value, otherwise if counter
     * is non-zero, decrement it.
     * </li>
     * <li>If control flag is clear, clear halt flag.</li>
     */
    @Override
    public void tick() {
        if (this.halt) {
            this.counter = this.reloadValue;
        } else {
            if (this.counter != 0) {
                this.counter--;
            }
        }
        this.halt = this.control;
    }
}
