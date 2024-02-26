package cn.navclub.nes4j.bin.apu.impl.timer;

import cn.navclub.nes4j.bin.apu.Timer;
import cn.navclub.nes4j.bin.apu.impl.TriangleChannel;
import cn.navclub.nes4j.bin.apu.impl.sequencer.TriangleSequencer;

/**
 * Triangle timer
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class TriangleTimer extends Timer<TriangleSequencer> {
    private final TriangleChannel channel;

    @SuppressWarnings("all")
    public TriangleTimer(TriangleSequencer sequencer, TriangleChannel channel) {
        super(sequencer);
        this.channel = channel;
    }

    @Override
    public void tick() {
        if (this.counter == 0) {
            this.counter = this.period;
        } else {
            if (this.counter > 0)
                this.counter--;
            //
            // When the timer generates a clock and the Length Counter and Linear Counter both
            // have a non-zero count, the sequencer is clocked.
            //
            if (this.counter == 0) {
                var linearCounter = this.channel.getLinearCounter();
                var lengthCounter = this.channel.getLengthCounter();
                if (channel.isEnable() && lengthCounter.getCounter() != 0 && linearCounter.getCounter() != 0) {
                    this.sequencer.tick();
                }
            }
        }
    }
}
