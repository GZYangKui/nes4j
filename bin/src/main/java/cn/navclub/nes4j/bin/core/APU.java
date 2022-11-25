package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.Component;
import cn.navclub.nes4j.bin.apu.FrameCounter;
import cn.navclub.nes4j.bin.apu.impl.DMChannel;
import cn.navclub.nes4j.bin.apu.impl.NoiseChannel;
import cn.navclub.nes4j.bin.apu.impl.PulseChannel;
import cn.navclub.nes4j.bin.apu.impl.TriangleChannel;
import lombok.Getter;
import lombok.Setter;

/**
 * <a href="https://www.nesdev.org/wiki/APU">APU Document</a>
 */
public class APU implements Component {
    private static final float[] PULSE_TABLE;
    private static final float[] TND_TABLE;


    static {
        TND_TABLE = new float[203];
        PULSE_TABLE = new float[31];

        for (int i = 0; i < 203; i++) {
            if (i < 31) {
                PULSE_TABLE[i] = (float) (95.52 / ((8128.0) / (float) i + 100));
            }
            TND_TABLE[i] = (float) (163.67 / (24329.0 / (float) i + 100));
        }
    }

    @Getter
    private final Bus bus;
    private final DMChannel dmc;
    private final NoiseChannel noise;
    private final PulseChannel pulse;
    private final PulseChannel pulse1;
    private final TriangleChannel triangle;
    private final FrameCounter frameCounter;

    private int index;
    private final float[] samples;

    public APU(Bus bus) {
        this.bus = bus;
        this.samples = new float[4096];
        this.dmc = new DMChannel(this);
        this.noise = new NoiseChannel(this);
        this.triangle = new TriangleChannel(this);
        this.frameCounter = new FrameCounter();
        this.pulse = new PulseChannel(this, PulseChannel.PulseIndex.PULSE_0);
        this.pulse1 = new PulseChannel(this, PulseChannel.PulseIndex.PULSE_1);
    }

    @Override
    public void write(int address, byte b) {
        //
        //When $4015 is written to, the channels' length counter enable flags are set,
        //the DMC is possibly started or stopped, and the DMC's IRQ occurred flag is
        //cleared.
        //
        //    ---d nt21   DMC, noise, triangle, square 2, square 1
        //
        //If d is set and the DMC's DMA reader has no more sample bytes to fetch, the DMC
        //sample is restarted. If d is clear then the DMA reader's sample bytes remaining
        //is set to 0.
        //
        if (address == 0x4015) {

            this.pulse.setEnable((b & 0x01) == 0x01);
            this.pulse1.setEnable((b & 0x02) == 0x02);
            this.triangle.setEnable((b & 0x04) == 0x04);
            this.noise.setEnable((b & 0x08) == 0x08);

            var dmcEnable = (b & 0x10) == 0x10;
            if (!dmcEnable) {
                this.dmc.setCurrentLength(0);
            }
            if (dmcEnable && this.dmc.getCurrentLength() == 0) {
                this.dmc.reset();
            }
            this.dmc.setInterrupt(false);
        }
        //0x4000-0x4003 Square Channel1
        else if (address >= 0x4000 && address <= 0x4003) {
            this.pulse.write(address, b);
        }
        //0x4004-0x4007  Square Channel2
        else if (address >= 0x4004 && address <= 0x4007) {
            this.pulse1.write(address, b);
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
        //Frame sequencer
        else if (address == 0x4017) {
            this.frameCounter.write(address, b);
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
        //    triangle length counter > 0
        //    square 2 length counter > 0
        //    square 1 length counter > 0
        //
        var value = 0;
        var c0 = pulse.getLengthCounter().getCounter();
        var c1 = pulse1.getLengthCounter().getCounter();
        var c3 = triangle.getLengthCounter().getCounter();
        var interrupt = this.frameCounter.isInterrupt();

        value |= (c0 > 0 ? 0x01 : 0x00);
        value |= (c1 > 0 ? 0x02 : 0x00);
        value |= (c3 > 0 ? 0x04 : 0x00);
        value |= (this.dmc.getCurrentLength() > 0 ? 0x10 : 0x00);
        value |= interrupt ? 0x40 : 0x00;
        value |= this.dmc.isInterrupt() ? 0x80 : 0x00;

        this.frameCounter.setInterrupt(false);

        return (byte) value;
    }

    private long cycle;

    @Override
    public void tick() {
        this.cycle++;

        if (this.cycle % 2 == 0) {
            this.dmc.tick();
            this.pulse.tick();
            this.pulse1.tick();
            this.noise.tick();
        }

        this.triangle.tick();

        this.frameCounter.tick();

        if (this.frameCounter.isOutput()) {
            var index = this.frameCounter.getIndex() - 1;
            if (index % 2 != 0) {
                this.pulse.lengthTick();
                this.pulse1.lengthTick();
                this.noise.lengthTick();
                this.triangle.lengthTick();
            }

            this.pulse.getEnvelope().tick();
            this.noise.getEnvelope().tick();
            this.pulse1.getEnvelope().tick();
            this.triangle.getLinearCounter().tick();
        }

        if ((this.cycle / 2) % 41 == 0) {
            this.samples[index++] = this.lookupSample();
            if (this.index >= this.samples.length) {
                this.index = 0;
                this.play(this.samples);
            }
        }
    }

    /**
     *
     *
     * Implementation Using Lookup Table
     * ---------------------------------
     * The formulas can be efficiently implemented using two lookup tables: a 31-entry
     * table for the two square channels and a 203-entry table for the remaining
     * channels (due to the approximation of tnd_out, the numerators are adjusted
     * slightly to preserve the normalized output range).
     *
     *     square_table [n] = 95.52 / (8128.0 / n + 100)
     *
     *     square_out = square_table [square1 + square2]
     *
     * The latter table is approximated (within 4%) by using a base unit close to the
     * DMC's DAC.
     *
     *     tnd_table [n] = 163.67 / (24329.0 / n + 100)
     *
     *     tnd_out = tnd_table [3 * triangle + 2 * noise + dmc]
     *
     *
     */
    private float lookupSample() {
        var d0 = this.dmc.output();
        var n0 = this.noise.output();
        var p0 = this.pulse.output();
        var p1 = this.pulse1.output();
        var t0 = this.triangle.output();

        var seqOut = PULSE_TABLE[p0 + p1];
        var tndOut = TND_TABLE[3 * t0 + 2 * n0 + d0];


        return tndOut + seqOut;
    }

    /**
     * 调用Native模块关闭音频相关资源
     */
    public synchronized native void stop();

    /**
     * 调用Native模块播放音频样本
     *
     * @param samples 音频样本
     */
    private native void play(float[] samples);
}
