package cn.navclub.nes4j.bin.apu.impl.sequencer;

import cn.navclub.nes4j.bin.apu.Sequencer;

public class TriangleSequencer implements Sequencer {
    private final int[] sequencer = new int[]{
            0x0f,
            0x0e,
            0x0d,
            0x0c,
            0x0b,
            0x0a,
            0x09,
            0x08,
            0x07,
            0x06,
            0x05,
            0x04,
            0x03,
            0x02,
            0x01,
            0x00,
            0x00,
            0x01,
            0x02,
            0x03,
            0x04,
            0x05,
            0x06,
            0x07,
            0x08,
            0x09,
            0x0a,
            0x0b,
            0x0c,
            0x0d,
            0x0e,
            0x0f
    };

    private int index;

    @Override
    public int value() {
        return this.sequencer[this.index];
    }

    @Override
    public void tick() {
        this.index = (this.index + 1) % sequencer.length;
    }
}
