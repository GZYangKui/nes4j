package cn.navclub.nes4j.bin.apu.impl.sequencer;

import cn.navclub.nes4j.bin.apu.Sequencer;
import lombok.Setter;

public class SeqSequencer implements Sequencer {
    private final int[][] sequences = new int[][]{
            {0, 0, 0, 0, 0, 0, 0, 1},
            {0, 0, 0, 0, 0, 0, 1, 1},
            {0, 0, 0, 0, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 0, 0}
    };
    @Setter
    private int duty;
    private int index;

    @Override
    public void tick(int cycle) {
        this.index = (this.index + 1) % 8;
    }

    @Override
    public int value() {
        return this.sequences[this.duty][this.index];
    }
}
