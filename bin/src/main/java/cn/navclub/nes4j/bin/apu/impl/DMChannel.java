package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.Sequencer;
import lombok.Getter;
import lombok.Setter;

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
    // 6-7	this is the playback mode.
    //
    //	00 - play DMC sample until length counter reaches 0 (see $4013)
    //	x1 - loop the DMC sample (x = immaterial)
    //	10 - play DMC sample until length counter reaches 0, then generate a CPU IRQ
    enum PlaybackMode {
        //  If playback mode "00" is chosen, the sample plays until the length counter
        //  reaches 0. No interrupt is generated.
        _00,
        //  Looping (playback mode "x1") will have the chunk of memory played over and
        //  over, until the channel is disabled (via $4015). In this case, after the
        //  length counter reaches 0, it will be reloaded with the calculated length
        //  value of $4013.
        _X1,
        //  If playback mode "10" is chosen, an interrupt will be dispached when the
        //  length counter reaches 0 (after the sample is done playing). There are 2
        //  ways to acknowledge the DMC's interrupt request upon recieving it. The first
        //  is a write to this register ($4010), with the MSB (bit 7) cleared (0). The
        //  second is any write to $4015 (see the $4015 register description for more
        //  details).
        _10
    }

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
    private byte sample;
    private byte bitCount;
    private byte shiftReg;
    private int sampleLength;
    private int sampleAddress;
    //$4013: length of play code
    @Setter
    @Getter
    private int lCounter;
    //$4012: play code's starting address
    private int currentAddress;
    private byte dacLSB;
    private int frequency;
    //$4011: delta counter
    private int deltaCounter;
    //$4015: DMC/IRQ status
    @Setter
    @Getter
    private boolean IRQFlag;
    private boolean silence;
    private int downCounter;
    private PlaybackMode mode;

    public DMChannel(APU apu) {
        super(apu, null);
        this.reset();
    }

    @Override
    public void write(int address, byte b) {
        var c = uint8(b);
        //
        // The rate determines for how many CPU cycles happen between changes in the output level during automatic
        // delta-encoded sample playback. For example, on NTSC (1.789773 MHz), a rate of 428 gives a frequency of
        // 1789773/428 Hz = 4181.71 Hz. These periods are all even numbers because there are 2 CPU cycles in an APU
        // cycle. A rate of 428 means the output level changes every 214 APU cycles.
        //
        if (address == 0x4010) {
            var index = (c >> 6) & 3;
            if (index == 3) {
                index = 1;
            }
            this.mode = PlaybackMode.values()[index];
            //
            //  3-0	this is the DMC frequency control. Valid values are from 0 - F. The
            //  value of this register determines how many CPU clocks to wait before the DMA
            //  will fetch another byte from memory. The # of clocks to wait -1 is initially
            //  loaded into an internal 12-bit down counter. The down counter is then
            //  decremented at the frequency of the CPU (1.79MHz). The channel fetches the
            //  next DMC sample byte when the count reaches 0, and then reloads the count.
            //  This process repeats until the channel is disabled by $4015, or when the
            //  length counter has reached 0 (if not in the looping playback mode). The
            //  exact number of CPU clock cycles is as follows:
            //
            this.frequency = (FREQ_TABLE[c & 0x0f]);
            if (this.mode != PlaybackMode._10) {
                this.IRQFlag = false;
            }
        }
        //  $4011 - Delta counter load register
        //  -----------------------------------
        //
        //  bits
        //  ----
        //  7	appears to be unused
        //  1-6	the load inputs of the internal delta counter
        //  0	LSB of the DAC
        //
        //  A write to this register effectively loads the internal delta counter with a
        //  6 bit value, but can be used for 7 bit PCM playback. Bit 0 is connected
        //  directly to the LSB (bit 0) of the DAC, and has no effect on the internal
        //  delta counter. Bit 7 appears to be unused.
        //
        //  This register can be used to output direct 7-bit digital PCM data to the
        //  DMC's audio output. To use this register for PCM playback, the programmer
        //  would be responsible for making sure that this register is updated at a
        //  constant rate. The rate is completely user-definable. For the regular CD
        //  quality 44100 Hz playback sample rate, this register would have to be
        //  written to approximately every 40 CPU cycles (assuming the 2A03 is running @
        //  1.79 MHz).
        //
        if (address == 0x4011) {
            this.dacLSB = (byte) (c & 1);
            this.deltaCounter = (c >> 1) & 0x3f;
            this.sample = (byte) ((this.deltaCounter << 1) + this.dacLSB);
        }
        //
        // $4012 - DMA address load register
        //  ----------------------------
        //
        //  This register contains the initial address where the DMC is to fetch samples
        //  from memory for playback. The effective address value is $4012 shl 6 or
        //  0C000H. This register is connected to the load pins of the internal DMA
        //  address pointer register (counter). The counter is incremented after every
        //  DMA byte fetch. The counter is 15 bits in size, and has addresses wrap
        //  around from $FFFF to $8000 (not $C000, as you might have guessed). The DMA
        //  address pointer register is reloaded with the initial calculated address,
        //  when the DMC is activated from an inactive state, or when the length counter
        //  has arrived at terminal count (count=0), if in the looping playback mode.
        //
        if (address == 0x4012) {
            this.sampleAddress = 0xc000 | (c << 6);
        }
        //
        // $4013 - DMA length register
        //  ---------------------------
        //
        //  This register contains the length of the chunk of memory to be played by the
        //  DMC, and it's size is measured in bytes. The value of $4013 shl 4 is loaded
        //  into a 12 bit internal down counter, dubbed the length counter. The length
        //  counter is decremented after every DMA fetch, and when it arrives at 0, the
        //  DMC will take action(s) based on the 2 MSB of $4010. This counter will be
        //  loaded with the current calculated address value of $4013 when the DMC is
        //  activated from an inactive state. Because the value that is loaded by the
        //  length counter is $4013 shl 4, this effectively produces a calculated byte
        //  sample length of $4013 shl 4 + 1 (i.e. if $4013=0, sample length is 1 byte
        //  long; if $4013=FF, sample length is $FF1 bytes long).
        //
        if (address == 0x4013) {
            this.sampleLength = (c << 4) | 1;
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
        if (!this.enable) {
            return;
        }
        //  The channel fetches the
        //  next DMC sample byte when the count reaches 0, and then reloads the count.
        //  This process repeats until the channel is disabled by $4015, or when the
        //  length counter has reached 0 (if not in the looping playback mode).

        if (this.downCounter == 0) {
            if (!this.silence) {
                this.timerClock();
            }
            this.downCounter = this.frequency;
        } else {
            this.downCounter--;
        }
        //  When an output cycle ends, a new cycle is started as follows:
        //
        //  The bits-remaining counter is loaded with 8.
        //  If the sample buffer is empty, then the silence flag is set; otherwise, the silence flag is
        //  cleared and the sample buffer is emptied into the shift register.
        var cycleOutputFinish = (this.bitCount == 0);
        if (cycleOutputFinish) {
            // If the sample buffer is empty, then the silence flag is set; otherwise, the silence flag
            // is cleared and the sample buffer is emptied into the shift register.
            this.silence = this.lCounter == 0;
//            this.sample = (byte) (this.deltaCounter << 1 | this.dacLSB);
            this.sample = (byte) this.deltaCounter;
            if (!this.silence) {
                this.reader();
                this.bitCount = 8;
            }
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
    private void timerClock() {
        if ((this.shiftReg & 1) == 1) {
            if (this.deltaCounter < 126) {
                this.deltaCounter += 2;
            }
        } else {
            if (this.deltaCounter > 1) {
                this.deltaCounter -= 2;
            }
        }
        this.shiftReg >>= 2;
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

        var ctx = this.apu.getContext();

        ctx.setStall(4);

        this.shiftReg = ctx.I8Read(this.currentAddress);

        this.currentAddress += 1;

        //The address is incremented; if it exceeds $FFFF, it is wrapped around to $8000.
        if (this.currentAddress > 0xffff) {
            this.currentAddress = 0x8000;
        }

        // The bytes remaining counter is decremented;
        this.lCounter--;
        //
        // if it becomes zero and the loop flag is set, the sample is restarted (see above);
        // otherwise, if the bytes remaining counter becomes zero and the IRQ
        // enabled flag is set, the interrupt flag is set.
        //
        if (this.lCounter == 0) {
            if (this.mode == PlaybackMode._X1) {
                this.loopReader();
            } else if (this.mode == PlaybackMode._10) {
                this.IRQFlag = true;
            }
        }
    }

    /**
     * When a sample is (re)started, the current address is set to the sample address,
     * and bytes remaining is set to the sample length.
     */
    public void loopReader() {
        this.lCounter = sampleLength;
        this.currentAddress = sampleAddress;
    }

    @Override
    public void reset() {
        super.reset();

        this.dacLSB = 0;
        this.sample = 0;
        this.lCounter = 0;
        this.silence = true;
        this.downCounter = 0;
        this.IRQFlag = false;
        this.currentAddress = 0;
        this.mode = PlaybackMode._00;
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
        return uint8(this.sample);
    }

    @Override
    public int readState() {
        return this.lCounter == 0 ? 0 : 1 << 4;
    }


}
