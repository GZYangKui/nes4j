package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.core.APU;

public class DMC extends Channel {
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
    private int dac;
    private int address;
    private boolean loop;
    private boolean interrupt;


    public DMC(APU apu) {
        super(apu, null);
    }

    @Override
    public void write(int address, byte b) {
        if (address == 0x4010) {
            this.loop = (b & 0x70) != 0;
            this.interrupt = (b & 0x80) != 0;
            this.timer.setPeriod(FREQ_TABLE[b & 0x0f]);
        }
        if (address == 0x4011) {
            this.dac = b;
        }
        if (address == 0x4012) {
            this.address = Byte.toUnsignedInt(b);
        }
    }
}
