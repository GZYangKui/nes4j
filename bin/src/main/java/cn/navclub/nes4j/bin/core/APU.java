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
    private static final int SAMPLE_NUM = 20;

    private final DMC dmc;
    private final Noise noise;
    private final Pulse pulse;
    private final Pulse pulse1;
    private final double[] smaples;
    private final SRegister status;
    private final Triangle triangle;
    private final FrameCounter frameCounter;

    private int index;

    public APU() {
        this.dmc = new DMC(this);
        this.status = new SRegister();
        this.noise = new Noise(this);
        this.triangle = new Triangle(this);
        this.frameCounter = new FrameCounter();
        this.smaples = new double[SAMPLE_NUM];
        this.pulse = new Pulse(this, Pulse.PulseIndex.PULSE_0);
        this.pulse1 = new Pulse(this, Pulse.PulseIndex.PULSE_1);
    }

    @Override
    public void write(int address, byte b) {
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
        return this.status.bits;
    }

    @Override
    public void tick(int cycle) {
        this.frameCounter.tick(cycle);
        if (!this.frameCounter.isOutput()) {
            return;
        }
        this.pulse.tick(cycle);
        this.pulse1.tick(cycle);

        var p0 = this.pulse.output();
        var p1 = this.pulse1.output();

        var sum = p0 + p1;
        var out = 0d;
        if (sum != 0) {
            out = 95.88 / ((8128 / (double) sum) + 100);
        }

        if (this.isSupport()) {
            this.smaples[index++] = out;
            if (index >= SAMPLE_NUM) {
                this.index = 0;
                this.play(this.smaples);
            }
        }

    }

    private native void play(double[] samples);

    /**
     * 判断当前系统是否已实现播放音频
     */
    private native boolean isSupport();

    public boolean interrupt() {
        return this.frameCounter.isInterrupt();
    }

    public boolean readStatus(APUStatus status) {
        return this.status.contain(status);
    }
}
