package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NESystemComponent;
import cn.navclub.nes4j.bin.apu.FrameCounter;
import cn.navclub.nes4j.bin.apu.impl.Pulse;
import cn.navclub.nes4j.bin.enums.APUStatus;

/**
 * <a href="https://www.nesdev.org/wiki/APU">APU Document</a>
 */
public class APU implements NESystemComponent {
    private final Pulse pulse;
    private final Pulse pulse1;
    private final SRegister status;
    private final FrameCounter frameCounter;

    public APU() {
        this.status = new SRegister();
        this.frameCounter = new FrameCounter();
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

        if (address == 0x4017) {
            this.frameCounter.write(address, b);
        }
    }

    @Override
    public byte read(int address) {
        return this.status.bits;
    }

    @Override
    public void tick(int cycle) {
        this.frameCounter.tick(cycle);
        if (this.frameCounter.isOutput()) {
            this.pulse.tick(cycle);
            this.pulse1.tick(cycle);
        }
    }

    public boolean interrupt() {
        return this.frameCounter.isInterrupt();
    }

    public boolean readStatus(APUStatus status) {
        return this.status.contain(status);
    }
}
