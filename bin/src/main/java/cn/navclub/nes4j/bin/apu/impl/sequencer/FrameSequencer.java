package cn.navclub.nes4j.bin.apu.impl.sequencer;

import cn.navclub.nes4j.bin.apu.Sequencer;
import lombok.Getter;
import lombok.Setter;

public class FrameSequencer implements Sequencer {
    @Setter
    private int mode;
    //2*CPU cycle=APU cycle
    private int cycle;
    @Getter
    private int index;
    @Getter
    @Setter
    private boolean output;

    private final int[][] sequencers = new int[][]{
            {7457, 7456, 7458, 7458},
            {7457, 7456, 7458, 14910}
    };

    public FrameSequencer() {

    }

    @Override
    public int value() {
        return this.sequencers[this.mode][index];
    }

    @Override
    public void tick(int cycle) {
        this.cycle += cycle;
        var stepValue = value();
        if (this.cycle >= stepValue) {
            this.index += 1;
            this.index %= 4;
            this.output = true;
            this.cycle %= stepValue;
        }
    }
}
