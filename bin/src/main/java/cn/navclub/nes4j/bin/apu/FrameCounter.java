package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.core.Component;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * <p>
 * The <b>NES APU frame counter</b> (or <b>frame sequencer</b>) generates low-frequency clocks for the channels
 * and an optional 60 Hz interrupt. The name "frame counter" might be slightly misleading because the clocks have
 * nothing to do with the video signal.
 * </p>
 * <p>
 * The <a href="https://www.nesdev.org/wiki/APU_Frame_Counter">frame counter</a> contains the following: divider, looping clock sequencer, frame interrupt flag.
 * </p>
 * <p>
 * The sequencer is clocked on every other CPU cycle, so 2 CPU cycles = 1 APU cycle. The sequencer keeps track
 * of how many APU cycles have elapsed in total, and each step of the sequence will occur once that total has
 * reached the indicated amount (with an additional delay of one CPU cycle for the quarter and half frame signals).
 * Once the last step has executed, the count resets to 0 on the next APU cycle.
 * </p>
 * <p>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@Slf4j
public class FrameCounter implements Component {
    @Setter
    @Getter
    //Whether happen interrupt
    private boolean interrupt;
    //IRQ is disable
    private boolean IRQDisable;
    @Setter
    private int mode;
    //2*CPU cycle=APU cycle
    private int cycle;
    private int index;
    private final APU apu;
    private final int[][] sequencers;
    private final Consumer<Integer> consumer;

    public FrameCounter(APU apu, Consumer<Integer> consumer) {
        this.index = 1;
        this.apu = apu;
        this.consumer = consumer;
        this.sequencers = new int[][]{
                {7457, 7456, 7458, 7458},
                {7457, 7456, 7458, 14910}
        };
    }

    /**
     * <pre>
     * On a write to $4017, the divider and sequencer are reset, then the sequencer is
     * configured. Two sequences are available, and frame IRQ generation can be
     * disabled.
     *
     *     mi-- ----       mode, IRQ disable
     *
     * If the mode flag is clear, the 4-step sequence is selected, otherwise the
     * 5-step sequence is selected and the sequencer is immediately clocked once.
     *
     *     f = set interrupt flag
     *     l = clock length counters and sweep units
     *     e = clock envelopes and triangle's linear counter
     *
     * mode 0: 4-step  effective rate (approx)
     * ---------------------------------------
     *     - - - f      60 Hz
     *     - l - l     120 Hz
     *     e e e e     240 Hz
     *
     * mode 1: 5-step  effective rate (approx)
     * ---------------------------------------
     *     - - - - -   (interrupt flag never set)
     *     l - l - -    96 Hz
     *     e e e e -   192 Hz
     *
     * At any time if the interrupt flag is set and the IRQ disable is clear, the
     * CPU's IRQ line is asserted.
     * </pre>
     */
    @Override
    public void write(int address, byte b) {
        this.index = 1;
        this.cycle = 0;
        //Sequencer mode: 0 selects 4-step sequence, 1 selects 5-step sequence.
        this.mode = ((b & 0x80) >> 7);
        //Interrupt inhibit flag. If set, the frame interrupt flag is cleared, otherwise it is unaffected.
        this.IRQDisable = (b & 0x40) == 0x40;
        if (this.IRQDisable) {
            this.interrupt = false;
        }
    }

    @Override
    public void tick() {
        this.cycle++;
        var value = this.sequencers[this.mode][this.index - 1];
        var output = this.cycle > value;
        if (output) {
            //Touch current step
            this.consumer.accept(this.index);
            //Next step
            this.index = (this.index + 1) % 5;

            if (this.index == 0) {
                this.index += 1;
            }

            this.cycle %= value;
            //
            // At any time if the interrupt flag is set and the IRQ disable is clear, the
            // CPU's IRQ line is asserted.
            //
            if (this.index == 4 && this.mode == 0 && !this.IRQDisable) {
                this.interrupt = true;
                this.apu.fireIRQ();
            }

        }
    }

    @Override
    public byte read(int address) {
        return 0;
    }
}
