package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import cn.navclub.nes4j.bin.enums.ChannelType;
import lombok.Getter;

/**
 * 音频通道
 */
@Getter
public class Channel implements CycleDriver {
    protected int volume;
    protected int lengthCounter;
    protected int duty;
    protected boolean envelopeLoop;
    protected boolean constantVolume;
    protected int envelopValue;
    protected int envelopCounter;
    protected boolean sweepEnable;
    protected int sweepPeriod;
    protected int sweepCounter;
    protected int timer;
    protected int internalTimer;
    protected int counter;
    protected boolean enable;
    protected final ChannelType type;


    public Channel(ChannelType type) {
        this.type = type;
    }

    public void update(int address, byte b) {
        var pos = address % type.getOffset();
    }


    @Override
    public void tick(int cycle) {
        if (!this.enable) {
            return;
        }
        if (this.internalTimer == 0) {
            this.internalTimer = this.timer;
        }
    }

    protected void stop() {
//        this.counter++;
//        if (!this.enab
//        le || this.lengthCounter == 0 || this.timer < 8 || this.timer > 0x7ff) {
//            this.volume = 0;
//        } else if (this.constantVolume) {
////            this.volume = this.envelopeValue * DUTY_TABLE[this.duty][this.counter & 0x07];
//        } else {
////            this.volume = this.envelopeVolume * DUTY_TABLE[this.duty][this.counter & 0x07];
//        }
    }
}
