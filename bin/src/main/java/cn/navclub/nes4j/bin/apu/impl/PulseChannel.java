package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.Envelope;
import cn.navclub.nes4j.bin.apu.SweepUnit;
import cn.navclub.nes4j.bin.apu.impl.sequencer.SeqSequencer;
import cn.navclub.nes4j.bin.core.APU;
import lombok.Getter;

public class PulseChannel extends Channel {
    @Getter
    private final PulseIndex index;
    private final Envelope envelope;
    private final SweepUnit sweepUnit;


    public PulseChannel(APU apu, PulseIndex index) {
        super(apu, new SeqSequencer());

        this.index = index;
        this.sweepUnit = new SweepUnit();
        this.envelope = new Envelope(this);
    }

    @Override
    public void write(int address, byte b) {
        var i = address % this.index.offset;
        this.value[i] = b;

        if (i == 0) {
            this.envelope.update(b);
            this.lengthCounter.setHalt((b & 0x20) != 0);
            //更新占空比
            ((SeqSequencer) (this.sequencer)).setDuty(b & 0x03);
        }

        //更新定时器周期
        if (i >= 2) {
            this.updateTimeValue();
        }

        //更新滑音单元
        if (i == 1) {
            this.sweepUnit.update(b);
        }

        if (i == 3) {
            this.lock = true;
            this.lengthCounter.lookupTable(b);
        }
    }


    @Override
    public void tick(int cycle) {
        this.envelope.tick(cycle);

        this.lock = false;

        if (apu.halfFrame()) {
            this.sweepUnit.tick(cycle);
            this.lengthCounter.tick(cycle);
            var value = this.sweepUnit.calculate(this.timer.getPeriod(), this.index);
            this.timer.setPeriod(value);
        }

        this.timer.tick(cycle);
    }

    /*
     *
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
     *
     */
    @Override
    public int output() {
        var value = this.envelope.getVolume();
        if (sequencer.value() == 0) {
            value = 0;
        }
        if (lengthCounter.getCounter() == 0) {
            value = 0;
        }
        return value;
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
