package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.core.Component;
import lombok.Getter;

import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

public abstract class Channel implements Component {
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
    public void tick() {
        this.timer.tick();
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

    public void lengthTick() {
        this.lengthCounter.tick();
    }

    /**
     * 当前通道输出
     */
    public abstract int output();

    /**
     * Update timer period
     *
     * @param address Register address
     * @param b       Register value
     */
    protected void updateTimeValue(int address, byte b) {
        if (this.timer == null) {
            return;
        }
        var first = address == 0x4002 || address == 0x4006 || address == 0x400a;
        var second = address == 0x4003 || address == 0x4007 || address == 0x400b;
        var update = first || second;
        if (update) {
            var value = this.timer.getPeriod();
            if (first) {
                value |= uint8(b);
            } else {
                value |= ((uint8(b) & 0x07) << 8);
            }
            this.timer.setPeriod(value);
        }

    }
}
