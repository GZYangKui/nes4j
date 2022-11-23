package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.core.APU;
import cn.navclub.nes4j.bin.enums.APUStatus;
import lombok.Getter;
import lombok.Setter;

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
    private byte shiftReg;
    private byte bitCount;
    private int sampleLength;
    private int sampleAddress;
    @Setter
    @Getter
    private int currentLength;
    private int currentAddress;

    private int timeValue;
    private int timePeriod;
    @Getter
    private boolean interrupt;


    public DMChannel(APU apu) {
        super(apu, null);
    }

    @Override
    public void write(int address, byte b) {
        if (address == 0x4010) {
            this.loop = (b & 0x40) != 0;
            this.interrupt = (b & 0x80) != 0;
            this.timePeriod = (FREQ_TABLE[b & 0x0f]);
        }
        if (address == 0x4011) {
            this.value = (byte) (b & 0x7f);
        }
        if (address == 0x4012) {
            this.sampleAddress = 0xc000 | ((b & 0xff) << 6);
        }
        if (address == 0x4013) {
            this.sampleLength = ((b & 0xff) << 4) | 1;
        }
    }

    @Override
    public void tick(int cycle) {
        if (!this.apu.readStatus(APUStatus.DMC)) {
            return;
        }
        this.stepReader();
        if (this.timeValue == 0) {
            this.timeValue = this.timePeriod;
            this.stepShift();
        } else {
            this.timeValue--;
        }
    }

    private void stepShift() {
        if (this.bitCount == 0) {
            return;
        }
        if ((this.shiftReg & 1) == 1) {
            if (this.value <= 125) {
                this.value += 2;
            }
        } else {
            if (this.value >= 2) {
                this.value -= 2;
            }
        }
        this.shiftReg >>= 2;
        this.bitCount--;
    }

    public void stepReader() {
        if (!(this.currentLength > 0 && this.bitCount == 0)) {
            return;
        }
        this.bitCount = 8;
        this.shiftReg = this.apu.getBus().read(this.currentAddress);
        this.currentAddress++;
        if (this.currentAddress == 0) {
            this.currentAddress = 0x8000;
        }
        this.currentLength--;
        if (this.currentLength == 0 && this.loop) {
            this.reset();
        }
    }

    public void reset() {
        this.currentLength = sampleLength;
        this.currentAddress = sampleAddress;
    }

    @Override
    public int output() {
        return this.value;
    }
}