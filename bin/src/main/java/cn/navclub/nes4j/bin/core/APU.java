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

    private static final int SAMPLE_NUM = 100;

    private final DMChannel dmc;
    private final NoiseChannel noise;
    private final PulseChannel pulse;
    private final PulseChannel pulse1;
    private final TriangleChannel triangle;
    private final FrameCounter frameCounter;
    private final boolean support;
    @Getter
    @Setter
    private Bus bus;

    public APU() {
        this.support = this.create();
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

            var dmc = (b & 0x10) == 0x10;
            if (!dmc) {
                this.dmc.setCurrentLength(0);
            }
            if (dmc && this.dmc.getCurrentLength() == 0) {
                this.dmc.reset();
            }
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

    @Override
    public void tick(int cycle) {
        this.frameCounter.tick(cycle);
        if (!this.frameCounter.next()) {
            return;
        }

        this.dmc.tick(cycle);
        this.pulse.tick(cycle);
        this.noise.tick(cycle);
        this.pulse1.tick(cycle);
        this.triangle.tick(cycle);

        if (!this.support) {
            return;
        }


        var d0 = this.dmc.output();
        var n0 = this.noise.output();
        var p0 = this.pulse.output();
        var p1 = this.pulse1.output();
        var t0 = this.triangle.output();

        var seqOut = PULSE_TABLE[p0 + p1];
        var tndOut = TND_TABLE[3 * t0 + 2 * n0 + d0];
        var out = tndOut + seqOut;

        play(new float[]{out});
    }

    public boolean halfFrame() {
        return this.frameCounter.seqIndex() % 2 == 0;
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

    /**
     * 判断当前系统是否已实现播放音频
     */
    private native boolean create();

    /**
     *
     *  判断当前模块是否触发IRQ中断
     *
     */
    public boolean interrupt() {
        return this.frameCounter.interrupt() || this.dmc.interrupt();
    }
}
