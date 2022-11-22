package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NESystemComponent;
import cn.navclub.nes4j.bin.apu.FrameCounter;
import cn.navclub.nes4j.bin.apu.impl.DMC;
import cn.navclub.nes4j.bin.apu.impl.Noise;
import cn.navclub.nes4j.bin.apu.impl.Pulse;
import cn.navclub.nes4j.bin.apu.impl.Triangle;
import cn.navclub.nes4j.bin.enums.APUStatus;

/**
 * <a href="https://www.nesdev.org/wiki/APU">APU Document</a>
 */
public class APU implements NESystemComponent {
    private static final int SAMPLE_NUM = 50;

    private final DMC dmc;
    private final Noise noise;
    private final Pulse pulse;
    private final Pulse pulse1;
    private final double[] samples;
    private final SRegister status;
    private final Triangle triangle;
    private final FrameCounter frameCounter;
    private final boolean support;
    private int index;

    public APU() {
        this.support = this.create();
        this.dmc = new DMC(this);
        this.status = new SRegister();
        this.noise = new Noise(this);
        this.triangle = new Triangle(this);
        this.frameCounter = new FrameCounter();
        this.samples = new double[SAMPLE_NUM];
        this.pulse = new Pulse(this, Pulse.PulseIndex.PULSE_0);
        this.pulse1 = new Pulse(this, Pulse.PulseIndex.PULSE_1);
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
            this.status.setBits(b);
        }

        if (address >= 0x4000 && address <= 0x4003) {
            this.pulse.write(address, b);
        }

        if (address >= 0x4004 && address <= 0x4007) {
            this.pulse1.write(address, b);
        }

        if (address >= 0x4008 && address <= 0x400b) {
            this.triangle.write(address, b);
        }

        if (address >= 0x400c && address <= 0x400f) {
            this.noise.write(address, b);
        }

        if (address >= 0x4010 && address <= 0x4013) {
            this.dmc.write(address, b);
        }

        if (address == 0x4017) {
            this.frameCounter.write(address, b);
        }
    }

    @Override
    public byte read(int address) {
        if (address != 0x4015) {
            throw new RuntimeException("Write-only register not only read operation.");
        }
        //
        //When $4015 is read, the status of the channels' length counters and bytes
        //remaining in the current DMC sample, and interrupt flags are returned.
        //Afterwards the Frame Sequencer's frame interrupt flag is cleared.
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
        value |= 0x10;
        value |= interrupt ? 0x40 : 0x00;
        value |= 0x80;

        this.frameCounter.setInterrupt(false);

        return (byte) value;
    }

    @Override
    public void tick(int cycle) {
        this.frameCounter.tick(cycle);
        if (!this.frameCounter.isOutput()) {
            return;
        }
        this.pulse.tick(cycle);
        this.noise.tick(cycle);
        this.pulse1.tick(cycle);
        this.triangle.tick(cycle);

        if (!this.support) {
            return;
        }


        var n0 = this.noise.output();
        var p0 = this.pulse.output();
        var p1 = this.pulse1.output();
        var t0 = this.triangle.output();

        var sum = p0 + p1;
        var pulseOut = 0d;
        if (sum != 0) {
            pulseOut = 95.88 / ((8128 / (double) sum) + 100);
        }

        var tndOut = 1 / ((t0 / 8227.0) + (n0 / 12241.0) + (120 / 22638));
        tndOut = 159.79 / (tndOut + 100);

        this.samples[index++] = (tndOut + pulseOut);
        if (index >= SAMPLE_NUM) {
            this.index = 0;
            play(this.samples);
        }
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
    private native void play(double[] samples);

    /**
     * 判断当前系统是否已实现播放音频
     */
    private native boolean create();

    public boolean interrupt() {
        return this.frameCounter.interrupt();
    }

    public boolean readStatus(APUStatus status) {
        return this.status.contain(status);
    }
}
