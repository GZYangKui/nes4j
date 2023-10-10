package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.core.Component;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.apu.impl.*;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import lombok.Getter;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

/**
 * <pre>
 *
 * The <a href="https://www.nesdev.org/wiki/APU">APU</a> is composed of five channels: square 1, square 2, triangle, noise,
 * delta modulation channel (DMC). Each has a variable-rate timer clocking a
 * waveform generator, and various modulators driven by low-frequency clocks from
 * a frame sequencer. The DMC plays samples while the other channels play
 * waveforms. The waveform channels have duration control, some have a volume
 * envelope unit, and a couple have a frequency sweep unit.
 *
 * Square 1/Square 2
 *
 * $4000/4 ddle nnnn   duty, loop env/disable length, env disable, vol/env
 * period
 * $4001/5 eppp nsss   enable sweep, period, negative, shift
 * $4002/6 pppp pppp   period low
 * $4003/7 llll lppp   length index, period high
 *
 * Triangle
 *
 * $4008   clll llll   control, linear counter load
 * $400A   pppp pppp   period low
 * $400B   llll lppp   length index, period high
 *
 * Noise
 *
 * $400C   --le nnnn   loop env/disable length, env disable, vol/env period
 * $400E   s--- pppp   short mode, period index
 * $400F   llll l---   length index
 *
 * DMC
 *
 * $4010   il-- ffff   IRQ enable, loop, frequency index
 * $4011   -ddd dddd   DAC
 * $4012   aaaa aaaa   sample address
 * $4013   llll llll   sample length
 *
 * Common
 *
 * $4015   ---d nt21   length ctr enable: DMC, noise, triangle, pulse 2, 1
 * $4017   fd-- ----   5-frame cycle, disable frame interrupt
 *
 * Status (read)
 *
 * $4015   if-d nt21   DMC IRQ, frame IRQ, length counter statuses
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class APU implements Component {
    private static final float[] TND_TABLE;
    private static final float[] PULSE_TABLE;

    static {
        TND_TABLE = new float[203];
        PULSE_TABLE = new float[31];

        for (int i = 0; i < 203; i++) {
            if (i < 31) {
                PULSE_TABLE[i] = (float) (95.52 / (8128.0 / i + 100));
            }
            TND_TABLE[i] = (float) (163.67 / (24329.0 / i + 100));
        }
    }

    @Getter
    private final NES context;
    private final DMChannel dmc;
    private final Player player;
    private final NoiseChannel noise;
    private final PulseChannel pulse1;
    private final PulseChannel pulse2;
    private final TriangleChannel triangle;
    private final FrameCounter frameCounter;

    public APU(NES context) {
        this.context = context;
        this.dmc = new DMChannel(this);
        this.noise = new NoiseChannel(this);
        this.triangle = new TriangleChannel(this);
        this.player = Player.newInstance(context.getPlayer());
        this.pulse1 = new PulseChannel(this, false);
        this.pulse2 = new PulseChannel(this, true);
        this.frameCounter = new FrameCounter(this, this::frameSequence);
    }

    @Override
    public void write(int address, byte b) {
        //
        // When $4015 is written to, the channels' length counter enable flags are set,
        // the DMC is possibly started or stopped, and the DMC's IRQ occurred flag is
        // cleared.
        //
        //    ---d nt21   DMC, noise, triangle, square 2, square 1
        //
        // If d is set and the DMC's DMA reader has no more sample bytes to fetch, the DMC
        // sample is restarted. If d is clear then the DMA reader's sample bytes remaining
        // is set to 0.
        //
        if (address == 0x4015) {

            this.pulse1.setEnable((b & 0x01) == 0x01);
            this.pulse2.setEnable((b & 0x02) == 0x02);
            this.triangle.setEnable((b & 0x04) == 0x04);
            this.noise.setEnable((b & 0x08) == 0x08);

            var enable = (b & 0x10) == 0x10;
            //
            // If the DMC bit is clear, the DMC bytes remaining will be set to 0 and the DMC will
            // silence when it empties.
            //
            if (!enable) {
                this.dmc.setCurrentLength(0);
            }
            //
            // If the DMC bit is set, the DMC sample will be restarted only if its bytes remaining is 0.
            // If there are bits remaining in the 1-byte sample buffer, these will finish playing before
            // the next sample is fetched.
            //
            if (enable && this.dmc.getCurrentLength() == 0) {
                this.dmc.reset();
            }
            this.dmc.setEnable(enable);
            //Writing to this register clears the DMC interrupt flag.
            this.dmc.setInterrupt(false);
        }
        //0x4000-0x4003 Square Channel1
        else if (address >= 0x4000 && address <= 0x4003) {
            this.pulse1.write(address, b);
        }
        //0x4004-0x4007  Square Channel2
        else if (address >= 0x4004 && address <= 0x4007) {
            this.pulse2.write(address, b);
        }
        //0x4008-0x400b Triangle channel
        else if (address >= 0x4008 && address <= 0x400b) {
            this.triangle.write(address, b);
        }
        //0x400c-0x400f Noise channel
        else if (address >= 0x400c && address <= 0x400f) {
            this.noise.write(address, b);
        }
        //0x4010-0x4013 DMC channel
        else if (address >= 0x4010 && address <= 0x4013) {
            this.dmc.write(address, b);
        }
        //Update frame counter
        else if (address == 0x4017) {
            this.frameCounter.write(address, b);
            this.tick();
        }
    }

    @Override
    public byte read(int address) {
        if (address != 0x4015) {
            throw new RuntimeException("Write-only register not only read operation.");
        }
        //
        // When $4015 is read, the status of the channels' length counters and bytes
        // remaining in the current DMC sample, and interrupt flags are returned.
        // Afterwards the Frame Sequencer's frame interrupt flag is cleared.
        //    if-d nt21
        //
        //    IRQ from DMC
        //    frame interrupt
        //    DMC sample bytes remaining > 0
        //    noise length counter > 0
        //    triangle length counter > 0
        //    square 2 length counter > 0
        //    square 1 length counter > 0
        //
        var value = 0;

        var c0 = pulse1.getLengthCounter().getCounter();
        var c1 = pulse2.getLengthCounter().getCounter();
        var c2 = triangle.getLengthCounter().getCounter();
        var c3 = this.noise.getLengthCounter().getCounter();


        value |= (c0 > 0 ? 1 : 0);
        value |= (c1 > 0 ? 1 << 1 : 0);
        value |= (c2 > 0 ? 1 << 2 : 0);
        value |= (c3 > 0 ? 1 << 3 : 0);
        value |= (this.dmc.getCurrentLength() > 0 ? 1 << 4 : 0);
        value |= this.frameCounter.isInterrupt() ? 1 << 6 : 0;
        value |= this.dmc.isInterrupt() ? 1 << 7 : 0;

        //Reading this register clears the frame interrupt flag (but not the DMC interrupt flag).
        this.frameCounter.setInterrupt(false);

        return int8(value);
    }

    private long cycle;

    @Override
    public void tick() {
        this.cycle++;

        if (this.cycle % 2 == 0) {
            this.dmc.tick();
            this.pulse1.tick();
            this.pulse2.tick();
            this.noise.tick();
        }

        this.triangle.tick();
        this.frameCounter.tick();
        // 1.79 MHz / 44100 = 40
        if ((this.cycle / 2) % 40 == 0) {
            var output = this.lookupSample();
            if (this.player != null && !this.context.isMute()) {
                this.player.output(output);
            }
        }

        // At any time, if the interrupt flag is set, the CPU's IRQ line is continuously asserted
        // until the interrupt flag is cleared. The processor will continue on from where it was stalled.
        if (this.dmc.isInterrupt()) {
            this.fireIRQ();
        }
    }

    /**
     * <p>
     * <b>Lookup Table</b>
     * </p>
     * <p>
     * The <a href="https://www.nesdev.org/wiki/APU_Mixe">APU mixer</a> formulas can be efficiently implemented using two lookup tables: a
     * 31-entry table for the two pulse channels and a 203-entry table for the remaining channels
     * (due to the approximation of tnd_out, the numerators are adjusted slightly to preserve the
     * normalized output range).
     * </p>
     * <pre>
     *     output = pulse_out + tnd_out
     *
     *     pulse_table [n] = 95.52 / (8128.0 / n + 100)
     *
     *     pulse_out = pulse_table [pulse1 + pulse2]
     * </pre>
     * <p>The tnd_out table is approximated (within 4%) by using a base unit close to the DMC's DAC.</p>
     * <pre>
     *     tnd_table [n] = 163.67 / (24329.0 / n + 100)
     *
     *     tnd_out = tnd_table [3 * triangle + 2 * noise + dmc]
     * </pre>
     */
    private byte lookupSample() {
        var d0 = this.dmc.output();
        var n0 = this.noise.output();
        var p1 = this.pulse1.output();
        var p2 = this.pulse2.output();
        var t0 = this.triangle.output();

        var seqOut = PULSE_TABLE[p2 + p1];
        var tndOut = TND_TABLE[3 * t0 + 2 * n0 + d0];
        var sample = tndOut + seqOut;
        return int8(Math.round(sample * 0xff));
    }


    public void fireIRQ() {
        this.context.interrupt(CPUInterrupt.IRQ);
    }

    @Override
    public void stop() {
        if (this.player != null) {
            this.player.stop();
        }
    }

    private void frameSequence(int index) {
        //
        // Length counters & sweep units
        // (Half frame)
        //
        if (index % 2 == 0 || index == -1) {
            this.pulse1.lengthTick();
            this.pulse2.lengthTick();
            this.noise.lengthTick();
            this.triangle.lengthTick();
        }
        //
        // Envelopes & triangle's linear counter
        // (Quarter frame)
        //
        this.noise.getEnvelope().tick();
        this.pulse1.getEnvelope().tick();
        this.pulse2.getEnvelope().tick();
        this.triangle.getLinearCounter().tick();
    }
}
