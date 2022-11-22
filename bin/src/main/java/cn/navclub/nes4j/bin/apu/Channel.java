package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.NESystemComponent;
import cn.navclub.nes4j.bin.core.APU;
import lombok.Getter;

public abstract class Channel implements NESystemComponent {
    @Getter
    protected final Timer timer;
    //储存四个寄存器的值
    protected byte[] value;
    protected final APU apu;
    @Getter
    protected boolean lock;
    protected final Sequencer sequencer;
    @Getter
    protected final LengthCounter lengthCounter;

    public Channel(final APU apu, final Sequencer sequencer) {
        this.apu = apu;
        this.value = new byte[4];
        this.sequencer = sequencer;
        this.timer = new Timer(this.sequencer);
        this.lengthCounter = new LengthCounter();
    }

    @Override
    public byte read(int address) {
        throw new RuntimeException("Write-only register.");
    }

    @Override
    public void tick(int cycle) {
        this.timer.tick(cycle);
        this.lengthCounter.tick(cycle);
    }

    /**
     * 当前通道输出
     */
    public int output() {
        return 0;
    }

    /**
     * 更新当前定时器的值
     */
    protected void updateTimeValue() {
        var lsb = this.value[2] & 0xff;
        var msb = this.value[3] & 0x07;
        var period = lsb | msb << 8;
        this.timer.setPeriod(period);
    }
}
