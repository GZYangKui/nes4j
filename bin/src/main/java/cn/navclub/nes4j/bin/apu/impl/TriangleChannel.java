package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.LinearCounter;
import cn.navclub.nes4j.bin.apu.impl.sequencer.TriangleSequencer;
import cn.navclub.nes4j.bin.core.APU;
import cn.navclub.nes4j.bin.enums.APUStatus;

public class TriangleChannel extends Channel {
    private final LinearCounter linearCounter;

    public TriangleChannel(APU apu) {
        super(apu, new TriangleSequencer());
        this.linearCounter = new LinearCounter();
    }

    @Override
    public void write(int address, byte b) {
        var index = address % 0x4008;
        this.value[index] = b;

        if (index == 0) {
            this.lengthCounter.setHalt((b & 0x80) != 0);
        }

        if (index >= 2) {
            this.updateTimeValue();
        }

        if (index == 3) {
            this.lengthCounter.lookupTable(b);
        }

        //When register $400B is written to, the halt flag is set.
        if (address == 0x400B) {
            this.linearCounter.setHalt(true);
        }

        if (address == 0x4008) {
            this.linearCounter.update(b);
        }
    }

    @Override
    public void tick(int cycle) {
        super.tick(cycle);
        this.linearCounter.tick(cycle);
    }

    /**
     * +---------+    +---------+
     * |LinearCtr|    | Length  |
     * +---------+    +---------+
     * |              |
     * v              v
     * +---------+        |\             |\         +---------+    +---------+
     * |  Timer  |------->| >----------->| >------->|Sequencer|--->|   DAC   |
     * +---------+        |/             |/         +---------+    +---------+
     */
    @Override
    public int output() {
        return sequencer.value();
    }
}
