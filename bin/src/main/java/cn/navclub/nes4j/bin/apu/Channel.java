package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.Component;
import cn.navclub.nes4j.bin.core.APU;
import lombok.Getter;

public abstract class Channel implements Component {
    @Getter
    protected boolean lock;
    @Getter
    protected Timer timer;
    @Getter
    protected boolean enable;
    @Getter
    protected Sequencer sequencer;
    protected final APU apu;
    @Getter
    protected final LengthCounter lengthCounter;


    public Channel(final APU apu, final Sequencer sequencer) {
        this.apu = apu;
        this.sequencer = sequencer;
        this.lengthCounter = new LengthCounter(this);
        this.timer = sequencer == null ? null : new Timer(this.sequencer);
    }

    @Override
    public byte read(int address) {
        throw new RuntimeException("Write-only register.");
    }

    @Override
    public void tick(int cycle) {
        this.timer.tick(cycle);
        if (this.apu.halfFrame()) {
            this.lengthCounter.tick(cycle);
        }
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
        //
        // Counting can be halted and the counter can be disabled by clearing the appropriate bit in the status register,
        // which immediately sets the counter to 0 and keeps it there.
        //
        if (!this.enable) {
            this.lengthCounter.setCounter(0);
        }
    }

    /**
     * 当前通道输出
     */
    public abstract int output();

    /**
     * 更新当前定时器的值
     */
    protected void updateTimeValue(int address, byte b) {
        if (this.timer == null) {
            return;
        }
        var first = address == 0x4002 || address == 0x4006 || address == 0x400A;
        var second = address == 0x4003 || address == 0x4007 || address == 0x400B;
        if (first || second) {
            var period = this.timer.getPeriod();
            period |= (second ? (b & 0x07) << 8 : b & 0xff);
            this.timer.setPeriod(period);
        }

    }
}
