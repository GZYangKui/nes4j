package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * <a href="https://www.nesdev.org/wiki/APU_Frame_Counter">Frame Counter</a>
 */
@Slf4j
public class FrameCounter implements Component {
    @Setter
    @Getter
    //Whether happen interrupt
    private boolean interrupt;
    //IEQ is disable
    private boolean IRQDisable;
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

    public FrameCounter() {
        this.index = 1;
    }

    @Override
    public void write(int address, byte b) {
        this.output = false;
        //Sequencer mode: 0 selects 4-step sequence, 1 selects 5-step sequence.
        this.mode = ((b & 0x80) >> 7);
        this.IRQDisable = (b & 0x40) != 0;
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void tick(int cycle) {
        this.cycle += cycle;
        var stepValue = this.sequencers[this.mode][this.index - 1];
        this.output = this.cycle >= stepValue;
        if (this.output) {
            this.index = this.index + 1;
            this.index = this.index % 5;
            if (this.index == 0) {
                this.index += 1;
            }
            this.cycle %= stepValue;
        }
    }

    public boolean interrupt() {
        var is = !this.IRQDisable && this.interrupt;
        //Clear interrupt avoid repeat invoke interrupt
        if (is) {
            this.interrupt = false;
        }
        return is;
    }
}
