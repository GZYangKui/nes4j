package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.NESystemComponent;
import cn.navclub.nes4j.bin.enums.MSequencer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * <a href="https://www.nesdev.org/wiki/APU_Frame_Counter">Frame Counter</a>
 */
@Slf4j
public class FrameCounter implements NESystemComponent {
    //0->4 1->5
    private MSequencer mode;
    @Getter
    //2*CPU cycle=APU cycle
    private int cycle;
    private int cursor;
    //Whether happen interrupt
    private boolean interrupt;
    //IEQ is disable
    private boolean IRQDisable;
    @Getter
    private boolean output;

    public FrameCounter() {
//        this.divider = new Divider();
        this.mode = MSequencer.FOUR_STEP_SEQ;
    }

    @Override
    public void write(int address, byte b) {
        this.cursor = 0;
        this.output = false;
        this.IRQDisable = (b & 0x40) != 0;
        //Sequencer mode: 0 selects 4-step sequence, 1 selects 5-step sequence.
        this.mode = (b & 0x80) == 0 ? MSequencer.FOUR_STEP_SEQ : MSequencer.FIVE_STEP_SEQ;
    }

    @Override
    public byte read(int address) {
        return 0;
    }

    @Override
    public void tick(int cycle) {
        this.cycle += cycle;
        this.output = false;
        var arr = this.mode.getSteps();

        if (this.cycle >= arr[this.cursor]) {
            this.interrupt = (mode == MSequencer.FOUR_STEP_SEQ) && (this.cursor == 0);
            this.cycle -= arr[this.cursor++];
            this.cursor %= 4;
            this.output = true;
        }
    }

    public boolean isInterrupt() {
        var is = !this.IRQDisable && this.interrupt;
        //Clear interrupt avoid repeat invoke interrupt
        if (is) {
            this.interrupt = false;
        }
        return is;
    }
}
