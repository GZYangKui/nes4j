package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 * Envelope Generate
 */
public class Envelope implements CycleDriver {
    private final Divider divider;
    private final Channel channel;

    private int volume;
    private int counter;
    private boolean loop;
    private boolean disable;


    public Envelope(final Channel channel) {
        this.channel = channel;
        this.divider = new Divider();
    }

    public void update(byte b) {
        this.volume = b & 0x0f;
        this.loop = (b & 0x20) != 0;
        this.disable = (b & 0x10) != 0;
        this.divider.setPeriod(volume + 1);
    }

    /**
     * When clocked by the frame sequencer, one of two actions occurs: if there was a
     * write to the fourth channel register since the last clock, the counter is set
     * to 15 and the divider is reset, otherwise the divider is clocked.
     */
    @Override
    public void tick(int cycle) {
        var lock = this.channel.lock;
        if (lock) {
            this.counter = 15;
            this.divider.reset();
        } else {
            this.divider.tick(cycle);
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
            return this.volume;
        }
        return this.counter;
    }
}
