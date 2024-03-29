package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.apu.impl.timer.Divider;
import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 * <p>
 * In a synthesizer, an envelope is the way a sound's parameter changes over time. The NES APU has an envelope generator
 * that controls the volume in one of two ways: it can generate a decreasing saw envelope (like a decay phase of an ADSR)
 * with optional looping, or it can generate a constant volume that a more sophisticated software envelope generator can
 * manipulate. Volume values are practically linear (see: APU Mixer).
 * </p>
 * <p>
 * Each volume envelope unit contains the following: start flag, divider, and decay level counter.
 * </p>
 * <pre>
 *                                        Loop flag
 *                                         |
 *                Start flag  +--------.   |   Constant volume
 *                            |        |   |        flag
 *                            v        v   v          |
 * Quarter frame clock --> Divider --> Decay --> |    |
 *                            ^        level     |    v
 *                            |                  | Select --> Envelope output
 *                            |                  |
 *         Envelope parameter +----------------> |
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class Envelope implements CycleDriver {
    private final Divider divider;

    private int counter;
    private int constant;
    private boolean loop;
    private boolean cflag;
    private boolean startFlag;


    public Envelope() {
        this.divider = new Divider((n) -> {
            // When the divider is clocked while at 0, it is loaded with V and clocks the decay level
            // counter. Then one of two actions occurs: If the counter is non-zero, it is decremented,
            // otherwise if the loop flag is set, the decay level counter is loaded with 15.
            if (this.counter > 0)
                this.counter--;
            else if (this.loop) {
                this.counter = 15;
            }
        });
    }

    public void update(byte b) {
        this.startFlag = true;
        this.constant = b & 0x0f;
        this.loop = (b & 0x20) == 0x20;
        this.cflag = (b & 0x10) == 0x10;
        this.divider.setPeriod(constant + 1);
    }

    /**
     * <p>
     * When clocked by the frame counter, one of two actions occurs: if the start flag is clear,
     * the divider is clocked, otherwise the start flag is cleared, the decay level counter is
     * loaded with 15, and the divider's period is immediately reloaded.
     * </p>
     * <p>
     * The envelope unit's volume output depends on the constant volume flag: if set, the envelope
     * parameter directly sets the volume, otherwise the decay level is the current volume. The
     * constant volume flag has no effect besides selecting the volume source; the decay level
     * will still be updated when constant volume is selected.
     * </p>
     * <p>
     * Each of the three envelope units' output is fed through additional gates at the sweep unit
     * (pulse only), waveform generator (sequencer or LFSR), and length counter.
     * </p>
     */
    @Override
    public void tick() {
        if (!this.startFlag) {
            this.divider.tick();
        } else {
            this.counter = 15;
            this.divider.reset();
            this.startFlag = false;
        }
    }

    /**
     * When disable is set, the channel's volume is n, otherwise it is the value in
     * the counter. Unless overridden by some other condition, the channel's DAC
     * receives the channel's volume value.
     */
    public int getVolume() {
        if (this.cflag) {
            return this.constant;
        }
        return this.counter;
    }

    /**
     * if there was a write to the fourth channel register since the last clock, the counter is set
     * to 15 and the divider is reset
     */
    public void resetLoop() {
        this.counter = 15;
        this.divider.reset();
    }

    public void reset() {
        this.counter = 0;
        this.constant = 0;
        this.loop = false;
        this.cflag = false;
        this.startFlag = false;
        this.divider.period = 0;
        this.divider.counter = 0;
    }

    /**
     * Because the envelope loop and length counter disable flags are mapped to the
     * same bit, the length counter can't be used while the envelope is in loop mode.
     * Similar applies to the triangle channel, where the linear counter and length
     * counter are both controlled by the same bit in register $4008.
     *
     * @return return {@code true} can safe use share flag bit otherwise can't use
     */
    public boolean shareFBit() {
        return this.cflag || !this.loop;
    }
}
