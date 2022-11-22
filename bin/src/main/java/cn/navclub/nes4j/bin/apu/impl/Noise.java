package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.Envelope;
import cn.navclub.nes4j.bin.core.APU;

public class Noise extends Channel {
    private static final int[] LOOK_TABLE = {
            0x004,
            0x008,
            0x010,
            0x020,
            0x040,
            0x060,
            0x080,
            0x0a0,
            0x0ca,
            0x0fe,
            0x17c,
            0x1fc,
            0x2fa,
            0x3f8,
            0x3f2,
            0xfe4
    };
    private final Envelope envelope;

    public Noise(APU apu) {
        super(apu,null);
        this.envelope = new Envelope(this);
    }

    @Override
    public void write(int address, byte b) {
        if (address == 0x400c) {
            this.envelope.update(b);
        }
        if (address == 0x400e) {
            var index = b & 0x0f;
            this.timer.setPeriod(LOOK_TABLE[index]);
        }
    }

    @Override
    public void tick(int cycle) {
        super.tick(cycle);
    }
}
