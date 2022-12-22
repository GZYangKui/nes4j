package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

/**
 * Envelope Generate
 */
public class Envelope implements CycleDriver {
    private final Divider divider;

    private int constant;
    private int counter;
    @Getter
    private boolean loop;
    private boolean disable;
    @Setter
    private boolean lock;


    public Envelope() {
        this.divider = new Divider();
    }

    public void update(byte b) {
        this.constant = b & 0x0f;
        this.loop = (b & 0x20) != 0;
        this.disable = (b & 0x10) != 0;
        this.divider.setPeriod(constant + 1);
    }

    /**
     * When clocked by the frame sequencer, one of two actions occurs: if there was a
     * write to the fourth channel register since the last clock, the counter is set
     * to 15 and the divider is reset, otherwise the divider is clocked.
     */
    @Override
    public void tick() {
        if (this.lock) {
            this.counter = 15;
            this.lock = false;
            this.divider.reset();
        } else {
            this.divider.tick();
        }
        //
        // When the divider outputs a clock, one of two actions occurs: if loop is set and
        // counter is zero, it is set to 15, otherwise if counter is non-zero, it is
        // decremented.
        //
        if (this.divider.output()) {
            if (this.loop && this.counter == 0)
                this.counter = 15;
            else {
                this.counter -= (this.counter != 0 ? 1 : 0);
            }
        }
    }

    /**
     * When disable is set, the channel's volume is n, otherwise it is the value in
     * the counter. Unless overridden by some other condition, the channel's DAC
     * receives the channel's volume value.
     */
    public int getVolume() {
        if (this.disable) {
            return this.constant;
        }
        return this.counter;
    }
}
