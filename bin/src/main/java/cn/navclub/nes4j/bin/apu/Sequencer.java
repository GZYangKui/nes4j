package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;

public abstract class Sequencer implements CycleDriver {
    protected int index;
    protected final int[] sequence;

    public Sequencer(int[] sequence) {
        this.sequence = sequence;
    }

    @Override
    public void tick(int cycle) {
        this.index = (this.index + 1) % this.sequence.length;
    }

    /**
     * 获取当前序列产生器值
     */
    public int value() {
        return this.sequence[this.index];
    }
}
