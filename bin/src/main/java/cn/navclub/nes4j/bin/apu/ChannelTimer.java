package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import cn.navclub.nes4j.bin.apu.impl.PSequencer;
import lombok.Setter;

public class ChannelTimer implements CycleDriver {
    private int counter;
    @Setter
    private int reload;

    private final PSequencer sequencer;

    public ChannelTimer() {
        this.sequencer = new PSequencer();
    }

    @Override
    public void tick(int cycle) {
        if (counter > 0) {
            counter--;
        } else {
            counter = this.reload;
            //生成波形
            this.sequencer.tick(cycle);
        }
    }
}
