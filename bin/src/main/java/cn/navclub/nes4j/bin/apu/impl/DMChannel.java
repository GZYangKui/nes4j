package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.apu.Sequencer;
import lombok.Getter;
import lombok.Setter;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;
import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * <p>
 * The <a href="https://www.nesdev.org/wiki/APU_DMC">DMC channel</a> contains the following: memory reader,
 * interrupt flag, sample buffer, timer, output unit, 7-bit output level with up and down counter.
 * </p>
 * <pre>
 *                           Timer
 *                            |
 *                            v
 * Reader ---> Buffer ---> Shifter ---> Output level ---> (to the mixer)
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class DMChannel extends Channel<Sequencer> {
    private final static int[] FREQ_TABLE = {
            0x1AC,
            0x17C,
            0x154,
            0x140,
            0x11E,
            0x0FE,
            0x0E2,
            0x0D6,
            0x0BE,
            0x0A0,
            0x08E,
            0x080,
            0x06A,
            0x054,
            0x048,
            0x036,
    };
    private byte value;
    private boolean loop;
    private byte shifter;
    private byte bitCount;
    private int sampleLength;
    private int sampleAddress;
    @Setter
    @Getter
    private int currentLength;
    private int currentAddress;

    private int period;
    private int counter;

    //IRQ whether enable
    private boolean inhibit;

    @Getter
    @Setter
    private boolean interrupt;


    public DMChannel(APU apu) {
        super(apu, null);
    }

    @Override
    public void write(int address, byte b) {
        //
        // The rate determines for how many CPU cycles happen between changes in the output level during automatic
        // delta-encoded sample playback. For example, on NTSC (1.789773 MHz), a rate of 428 gives a frequency of
        // 1789773/428 Hz = 4181.71 Hz. These periods are all even numbers because there are 2 CPU cycles in an APU
        // cycle. A rate of 428 means the output level changes every 214 APU cycles.
        //
        if (address == 0x4010) {
            this.loop = (b & 0x40) == 0x40;
            this.inhibit = (b & 0x80) == 0x80;
            this.period = (FREQ_TABLE[b & 0x0f]);
        }
        //
        // A write to $4011 sets the counter and DAC to a new value:
        //
        //    -ddd dddd       new DAC value
        //
        if (address == 0x4011) {
            this.value = int8(b & 0x7f);
        }
        //
        // Sample address
        // bits 7-0	AAAA.AAAA	Sample address = %11AAAAAA.AA000000 = $C000 + (A * 64)
        //
        if (address == 0x4012) {
            this.sampleAddress = 0xc000 | (uint8(b) << 6);
        }
        //
        // Sample length
        // Sample length = %LLLL.LLLL0001 = (L * 16) + 1 bytes
        //
        if (address == 0x4013) {
            this.sampleLength = (uint8(b) << 4) | 1;
        }
    }

    /**
     * <b>Output unit</b>
     * <p>
     * The output unit continuously outputs a 7-bit value to the mixer. It contains an 8-bit
     * right shift register, a bits-remaining counter, a 7-bit output level (the same one that
     * can be loaded directly via $4011), and a silence flag.
     * </p>
     * <p>
     * The bits-remaining counter is updated whenever the timer outputs a clock, regardless
     * of whether a sample is currently playing. When this counter reaches zero, we say that
     * the output cycle ends. The DPCM unit can only transition from silent to playing at the
     * end of an output cycle.
     * </p>
     * <p>
     * When an output cycle ends, a new cycle is started as follows:
     * </p>
     *
     * <li>The bits-remaining counter is loaded with 8.</li>
     * <li>
     * If the sample buffer is empty, then the silence flag is set; otherwise, the
     * silence flag is cleared and the sample buffer is emptied into the shift register.
     * </li>
     * <b>
     * Nothing can interrupt a cycle; every cycle runs to completion before a new cycle is started.
     * </b>
     */
    @Override
    public void tick() {
        this.reader();
        if (this.counter == 0) {
            this.clock();
            this.counter = this.period;
        } else {
            this.counter--;
        }
    }

    /**
     * <p>
     * When the timer outputs a clock, the following actions occur in order:
     * </p>
     * <li>
     * If the silence flag is clear, the output level changes based on bit 0 of the shift
     * register. If the bit is 1, add 2; otherwise, subtract 2. But if adding or subtracting
     * 2 would cause the output level to leave the 0-127 range, leave the output level unchanged.
     * This means subtract 2 only if the current level is at least 2, or add 2 only if the current
     * level is at most 125.
     * </li>
     * <li>
     * The right shift register is clocked.
     * </li>
     * <li>
     * As stated above, the bits-remaining counter is decremented. If it becomes zero, a new output cycle is started.
     * </li>
     */
    private void clock() {
        if (this.bitCount == 0) {
            return;
        }
        if ((this.shifter & 1) == 1) {
            if (this.value <= 125) {
                this.value += 2;
            }
        } else {
            if (this.value >= 2) {
                this.value -= 2;
            }
        }
        this.shifter >>= 2;
        this.bitCount--;
    }

    /**
     * <b>Memory reader</b>
     * <p>
     * When the sample buffer is emptied, the memory reader fills the sample buffer with the next byte from the currently playing sample. It has an address counter and a bytes remaining counter.
     * </p>
     * <p>
     * Any time the sample buffer is in an empty state and bytes remaining is not zero (including just after a write to $4015 that enables the channel, regardless of where that write occurs relative to the bit counter mentioned below), the following occur:
     * </p>
     * <p>
     * The CPU is stalled for up to 4 CPU cycles[2] to allow the longest possible write (the return address and write after an IRQ) to finish. If OAM DMA is in progress, it is paused for two cycles.[3] The sample fetch always occurs on an even CPU cycle due to its alignment with the APU. Specific delay cases:
     * </p>
     * <li>
     * 4 cycles if it falls on a CPU read cycle.
     * </li>
     * <li>
     * 3 cycles if it falls on a single CPU write cycle (or the second write of a double CPU write).
     * </li>
     * <li>
     * 4 cycles if it falls on the first write of a double CPU write cycle.[4]
     * </li>
     * <li>
     * 2 cycles if it occurs during an OAM DMA, or on the $4014 write cycle that triggers the OAM DMA.
     * </li>
     * <li>
     * 1 cycle if it occurs on the second-last OAM DMA cycle.
     * </li>
     * <li>
     * 3 cycles if it occurs on the last OAM DMA cycle.
     * </li>
     */
    public void reader() {
        if (this.currentLength > 0 && this.bitCount == 0) {
            this.bitCount = 8;
            var ctx = this.apu.getContext();

            ctx.setStall(4);

            this.shifter = ctx.I8Read(this.currentAddress);

            this.currentAddress += 1;

            //The address is incremented; if it exceeds $FFFF, it is wrapped around to $8000.
            if (this.currentAddress > 0xffff) {
                this.currentAddress = 0x8000;
            }

            //
            // The bytes remaining counter is decremented;
            // if it becomes zero and the loop flag is set, the sample is restarted (see above);
            // otherwise, if the bytes remaining counter becomes zero and the IRQ
            // enabled flag is set, the interrupt flag is set.
            //
            this.currentLength--;

            if (this.currentLength == 0 && this.loop) {
                this.reset();
            } else if (this.currentLength == 0 && this.inhibit) {
                this.interrupt = true;
            }
        }
    }

    /**
     * When a sample is (re)started, the current address is set to the sample address,
     * and bytes remaining is set to the sample length.
     */
    public void reset() {
        this.currentLength = sampleLength;
        this.currentAddress = sampleAddress;
    }

    /**
     * The output level is sent to the mixer whether the channel is enabled or not. It is loaded with 0 on power-up,
     * and can be updated by $4011 writes and delta-encoded sample playback.
     *
     * @return DMC DAC data
     */
    @Override
    public int output() {
        if (!this.enable) {
            return 0;
        }
        return uint8(this.value);
    }
}
