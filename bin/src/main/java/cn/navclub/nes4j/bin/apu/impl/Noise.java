package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.Envelope;
import cn.navclub.nes4j.bin.apu.impl.sequencer.NoiseSequencer;
import cn.navclub.nes4j.bin.core.APU;

public class Noise extends Channel {
    private static final int[] LOOK_TABLE = {
            0x004,
            0x008,
            0x010,
            0x020,
            0x040,
            0x060,
            0x080,
            0x0a0,
            0x0ca,
            0x0fe,
            0x17c,
            0x1fc,
            0x2fa,
            0x3f8,
            0x3f2,
            0xfe4
    };
    private final Envelope envelope;

    public Noise(APU apu) {
        super(apu, new NoiseSequencer());
        this.envelope = new Envelope(this);
    }

    @Override
    public void write(int address, byte b) {
        var i = address % 0x400c;
        this.value[i] = b;
        if (address == 0x400c) {
            this.envelope.update(b);
        }
        if (address == 0x400e) {
            var index = b & 0x0f;
            this.timer.setPeriod(LOOK_TABLE[index]);
            ((NoiseSequencer) this.sequencer).setMode((b & 0x80) >> 7);
        }
        if (i == 3) {
            this.lock = true;
        }
    }

    /**
     * +---------+    +---------+    +---------+
     * |  Timer  |--->| Random  |    | Length  |
     * +---------+    +---------+    +---------+
     * |              |
     * v              v
     * +---------+        |\             |\         +---------+
     * |Envelope |------->| >----------->| >------->|   DAC   |
     * +---------+        |/             |/         +---------+
     */
    @Override
    public int output() {
        var value = this.envelope.getVolume();
        if (this.sequencer.value() == 0) {
            value = 0;
        }
        if (lengthCounter.getCounter() == 0) {
            value = 0;
        }
        return value;
    }

    @Override
    public void tick(int cycle) {
        super.tick(cycle);
        this.envelope.tick(cycle);

        this.lock = false;
    }
}
