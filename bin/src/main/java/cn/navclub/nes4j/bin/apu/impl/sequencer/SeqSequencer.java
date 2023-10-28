package cn.navclub.nes4j.bin.apu.impl.sequencer;

import cn.navclub.nes4j.bin.apu.Sequencer;
import lombok.Setter;

/**
 * <b>Sequencer behavior</b>
 * <p>
 * The sequencer is clocked by an 11-bit timer. Given the timer value t = HHHLLLLLLLL formed by timer high and timer
 * low, this timer is updated every APU cycle (i.e., every second CPU cycle), and counts t, t-1, ..., 0, t, t-1, ...,
 * clocking the waveform generator when it goes from 0 to t. Since the period of the timer is t+1 APU cycles and the
 * sequencer has 8 steps, the period of the waveform is 8*(t+1) APU cycles, or equivalently 16*(t+1) CPU cycles.
 * </p>
 * <p>
 * <b>Hence</b>
 * <li>fpulse = fCPU/(16*(t+1)) (where fCPU is 1.789773 MHz for NTSC, 1.662607 MHz for PAL, and 1.773448 MHz for Dendy)</li>
 * <li>t = fCPU/(16*fpulse) - 1</li>
 * </p>
 * <p>
 * <b>Note:</b> A period of t < 8, either set explicitly or via a sweep period update, silences the corresponding pulse
 * channel. The highest frequency a pulse channel can output is hence about 12.4 kHz for NTSC. (TODO: PAL behavior?)
 * </p>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class SeqSequencer implements Sequencer {
    /**
     * <p>
     * The reason for the odd output from the sequencer is that the counter is initialized to zero
     * but counts downward rather than upward. Thus it reads the sequence lookup table in the order
     * 0, 7, 6, 5, 4, 3, 2, 1.
     * </p>
     */
    private final byte[][] sequences = new byte[][]{
            {0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 1, 1},
            {0, 0, 0, 0, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 0, 0}
    };
    @Setter
    private int duty;
    private int index;

    @Override
    public void tick() {
        this.index = (this.index + 1) % 8;
    }

    @Override
    public int value() {
        return this.sequences[this.duty][this.index];
    }

    @Override
    public void reset() {
        this.duty = 0;
        this.index = 0;
    }
}
