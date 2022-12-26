package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.LinearCounter;
import cn.navclub.nes4j.bin.apu.Timer;
import cn.navclub.nes4j.bin.apu.impl.sequencer.TriangleSequencer;
import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.apu.impl.timer.TriangleTimer;
import lombok.Getter;

public class TriangleChannel extends Channel<TriangleSequencer> {
    @Getter
    private final LinearCounter linearCounter;

    public TriangleChannel(APU apu) {
        super(apu);
        this.linearCounter = new LinearCounter();
        this.sequencer = new TriangleSequencer();
        this.timer = new TriangleTimer(this.sequencer, this);
    }

    @Override
    public void write(int address, byte b) {

        if (address == 0x4008) {
            this.linearCounter.update(b);
            if (!this.linearCounter.isControl()) {
            this.lengthCounter.setHalt((b & 0x80) != 0);
            }
        }

        if (address == 0x400B) {
            //When register $400B is written to, the halt flag is set.
            this.linearCounter.setHalt(true);
            if (this.enable) {
                this.lengthCounter.lookupTable(b);
            }
        }

        this.updateTimeValue(address, b);
    }

    /**
     * <pre>
     *                    +---------+    +---------+
     *                    |LinearCtr|    | Length  |
     *                    +---------+    +---------+
     *                         |              |
     *                         v              v
     *     +---------+        |\             |\         +---------+    +---------+
     *     |  Timer  |------->| >----------->| >------->|Sequencer|--->|   DAC   |
     *     +---------+        |/             |/         +---------+    +---------+
     * </pre>
     */
    @Override
    public int output() {
        if (!this.enable
                || this.timer.getPeriod() < 3
                || this.linearCounter.getCounter() == 0
                || this.lengthCounter.getCounter() == 0) {
            return 0;
        }
        return sequencer.value();
    }
}
