package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.Component;
import cn.navclub.nes4j.bin.apu.impl.sequencer.FrameSequencer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * <a href="https://www.nesdev.org/wiki/APU_Frame_Counter">Frame Counter</a>
 */
@Slf4j
public class FrameCounter implements Component {
    @Getter
    private int cursor;
    @Setter
    @Getter
    //Whether happen interrupt
    private boolean interrupt;
    //IEQ is disable
    private boolean IRQDisable;
    private final FrameSequencer sequencer;

    public FrameCounter() {
        this.sequencer = new FrameSequencer();
    }

    @Override
    public void write(int address, byte b) {
        this.cursor = 0;
        this.sequencer.setOutput(false);
        this.IRQDisable = (b & 0x40) != 0;
        //Sequencer mode: 0 selects 4-step sequence, 1 selects 5-step sequence.
        this.sequencer.setMode((b & 0x80) >> 7);
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void tick(int cycle) {
        this.sequencer.setOutput(false);
        this.sequencer.tick(cycle);
    }

    public boolean interrupt() {
        var is = !this.IRQDisable && this.interrupt;
        //Clear interrupt avoid repeat invoke interrupt
        if (is) {
            this.interrupt = false;
        }
        return is;
    }

    public boolean halfFrame() {
        var index = this.sequencer.getIndex() + 1;
        return index % 2 == 0;
    }

    public boolean next() {
        return this.sequencer.isOutput();
    }
}
