package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.Envelope;
import cn.navclub.nes4j.bin.apu.SweepUnit;
import cn.navclub.nes4j.bin.apu.impl.sequencer.SeqSequencer;
import cn.navclub.nes4j.bin.apu.APU;
import lombok.Getter;

/**
 * <p>
 * Each of the two <b>NES APU pulse</b> (square) wave channels generate a pulse wave with variable duty.
 * </p>
 * <p>
 * Each <a href="https://www.nesdev.org/wiki/APU_Pulse">pulse channel</a> contains the following:
 * </p>
 * <li>envelope generator</li>
 * <li>sweep unit</li>
 * <li>timer</li>
 * <li>8-step sequencer</li>
 * <li>length counter</li>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@Getter
public class PulseChannel extends Channel<SeqSequencer> {
    private final boolean second;
    private final Envelope envelope;
    private final SweepUnit sweepUnit;

    public PulseChannel(APU apu, boolean second) {
        super(apu, new SeqSequencer());

        this.second = second;
        this.envelope = new Envelope();
        this.sweepUnit = new SweepUnit(this);
    }

    @Override
    public void write(int address, byte b) {
        //$4000/4 ddle nnnn   duty, loop env/disable length, env disable, vol/env
        if (address == 0x4000 || address == 0x4004) {
            this.envelope.update(b);

            if (this.envelope.shareFBit()) {
                this.lengthCounter.setHalt((b & 0x20) != 0);
            }
            //Update duty
            this.sequencer.setDuty((b & 0xc0) >> 6);
        }

        //Update sweep properties
        if (address == 0x4001 || address == 0x4005) {
            this.sweepUnit.update(b);
        }

        if (address == 0x4003 || address == 0x4007) {
            this.envelope.resetLoop();
            if (this.enable) {
                this.lengthCounter.lookupTable(b);
            }
        }

        //Update timer period
        this.updateTimeValue(address, b);
    }

    /**
     * <pre>
     *                    +---------+    +---------+
     *                    |  Sweep  |--->|Timer / 2|
     *                    +---------+    +---------+
     *                         |              |
     *                         |              v
     *                         |         +---------+    +---------+
     *                         |         |Sequencer|    | Length  |
     *                         |         +---------+    +---------+
     *                         |              |              |
     *                         v              v              v
     *     +---------+        |\             |\             |\          +---------+
     *     |Envelope |------->| >----------->| >----------->| >-------->|   DAC   |
     *     +---------+        |/             |/             |/          +---------+
     * </pre>
     *
     * <p>
     * <b>Pulse channel output to mixer</b>
     * </p>
     * <p>
     * The mixer receives the pulse channel's current envelope volume (lower 4 bits from $4000 or $4004) except when
     * <li>The sequencer output is zero, or</li>
     * <li>overflow from the sweep unit's adder is silencing the channel, or</li>
     * <li>the length counter is zero, or</li>
     * <li>the timer has a value less than eight (t<8, noted above).</li>
     * If any of the above are true, then the pulse channel sends zero (silence) to the mixer.
     * </p>
     */
    @Override
    public int output() {
        if (!this.enable
                || this.sequencer.value() == 0
                || this.lengthCounter.silence()
                || this.sweepUnit.isSilence()) {
            return 0;
        }
        return this.envelope.getVolume();
    }

    @Override
    public void lengthTick() {
        super.lengthTick();
        this.sweepUnit.tick();
    }

    @Override
    public int readState() {
        return this.lengthCounter.stateVal() << (this.second ? 1 : 0);
    }

    @Override
    public void reset() {
        super.reset();
        this.envelope.reset();
        this.sweepUnit.reset();
    }
}
