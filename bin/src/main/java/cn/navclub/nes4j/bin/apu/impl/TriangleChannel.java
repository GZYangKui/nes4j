package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.LinearCounter;
import cn.navclub.nes4j.bin.apu.impl.sequencer.TriangleSequencer;
import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.apu.impl.timer.TriangleTimer;
import lombok.Getter;

/**
 * <p>
 * The NES APU triangle channel generates a pseudo-triangle wave. It has no volume control; the waveform is either
 * cycling or suspended. It includes a linear counter, an extra duration timer of higher accuracy than the length
 * counter.
 * </p>
 * <p>
 * The triangle channel contains the following: timer, length counter, linear counter, linear counter reload flag,
 * control flag, sequencer.
 * </p>
 * <pre>
 *      Linear Counter   Length Counter
 *             |                |
 *             v                v
 * Timer ---> Gate ----------> Gate ---> Sequencer ---> (to mixer)
 * </pre>
 */
@Getter
public class TriangleChannel extends Channel<TriangleSequencer> {
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
                this.lengthCounter.setHalt((b & 0x80) == 0x80);
            }
        }

        if (address == 0x400b) {
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
    // Silencing the triangle channel merely halts it. It will continue to output its last value rather than 0.
        return sequencer.value();
    }

    @Override
    public int readState() {
        return this.lengthCounter.stateVal() << 2;
    }

    @Override
    public void reset() {
        super.reset();
        this.linearCounter.reset();
    }
}
