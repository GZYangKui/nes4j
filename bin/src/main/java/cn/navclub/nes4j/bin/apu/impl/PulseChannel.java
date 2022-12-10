package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.Envelope;
import cn.navclub.nes4j.bin.apu.SweepUnit;
import cn.navclub.nes4j.bin.apu.impl.sequencer.SeqSequencer;
import cn.navclub.nes4j.bin.apu.APU;
import lombok.Getter;

public class PulseChannel extends Channel {
    @Getter
    private final PulseIndex index;
    @Getter
    private final Envelope envelope;
    @Getter
    private final SweepUnit sweepUnit;


    public PulseChannel(APU apu, PulseIndex index) {
        super(apu, new SeqSequencer());

        this.index = index;
        this.envelope = new Envelope(this);
        this.sweepUnit = new SweepUnit(this);
    }

    @Override
    public void write(int address, byte b) {
        if (address == 0x4000 || address == 0x4004) {
            this.envelope.update(b);
            //
            // Because the envelope loop and length counter disable flags are mapped to the
            // same bit, the length counter can't be used while the envelope is in loop mode.
            //
            if (!this.envelope.isLoop()) {
                this.lengthCounter.setHalt((b & 0x20) != 0);
            }
            //更新占空比
            ((SeqSequencer) (this.sequencer)).setDuty(b & 0x03);
        }

        //更新滑音单元
        if (address == 0x4001 || address == 0x4005) {
            this.sweepUnit.update(b);
        }

        if (address == 0x4003 || address == 0x4007) {
            this.lock = true;
            this.lengthCounter.lookupTable(b);
        }

        //更新定时器周期
        this.updateTimeValue(address, b);
    }

    /**
     *<pre>
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
     *</pre>
     */
    @Override
    public int output() {
        if (!this.enable || this.sequencer.value() == 0 || this.lengthCounter.getCounter() == 0) {
            return 0;
        }
        return this.envelope.getVolume();
    }

    @Override
    public void lengthTick() {
        super.lengthTick();
        this.sweepUnit.tick();
    }

    public enum PulseIndex {
        PULSE_0(0x4000),
        PULSE_1(0x4004);

        final int offset;

        PulseIndex(int offset) {
            this.offset = offset;
        }
    }
}
