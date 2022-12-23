package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.APU;
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
public class DMChannel extends Channel {
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

    @Getter
    @Setter
    private boolean IRQInterrupt;


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
            this.loop = (b & 0x40) != 0;
            this.IRQInterrupt = (b & 0x80) != 0;
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

    @Override
    public void tick() {
        if (!this.enable) {
            return;
        }
        this.stepReader();
        if (this.counter == 0) {
            this.stepShift();
            this.counter = this.period;
        } else {
            this.counter--;
        }
    }

    private void stepShift() {
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

    public void stepReader() {
        if (this.currentLength > 0 && this.bitCount == 0) {
            this.bitCount = 8;
            var ctx = this.apu.getContext();

            ctx.setStall(4);

            this.shifter = ctx.I8Read(this.currentAddress++);

            if (this.currentAddress == 0) {
                this.currentAddress = 0x8000;
            }

            this.currentLength--;

            if (this.currentLength == 0 && this.loop) {
                this.reset();
            }
        }
    }

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
