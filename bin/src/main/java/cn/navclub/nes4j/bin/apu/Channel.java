package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.core.Component;
import lombok.Getter;

import static cn.navclub.nes4j.bin.util.BinUtil.uint8;

/**
 * Abstract audio channel
 *
 * @param <T> Timer driver sequencer type
 * @see cn.navclub.nes4j.bin.apu.impl.PulseChannel
 * @see cn.navclub.nes4j.bin.apu.impl.TriangleChannel
 * @see cn.navclub.nes4j.bin.apu.impl.DMChannel
 * @see cn.navclub.nes4j.bin.apu.impl.NoiseChannel
 */
public abstract class Channel<T extends Sequencer> implements Component {
    protected final APU apu;
    @Getter
    protected final LengthCounter lengthCounter;

    @Getter
    protected Timer<T> timer;
    @Getter
    protected T sequencer;
    @Getter
    protected boolean enable;


    public Channel(final APU apu, T sequencer) {
        this.apu = apu;
        this.sequencer = sequencer;
        this.lengthCounter = new LengthCounter();
        this.timer = sequencer == null ? null : new Timer<>(this.sequencer);
    }

    public Channel(APU apu) {
        this(apu, null);
    }

    /**
     * Due to apu all register only write except when open bus <a href="https://www.nesdev.org/wiki/APU#Status_($4015)">status register</a>
     *
     * @param address {@inheritDoc}
     * @return {@inheritDoc}
     * @throws RuntimeException
     */
    @SuppressWarnings("all")
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
        // Counting can be halted and the counter can be disabled by clearing the appropriate bit in the status
        // register,which immediately sets the counter to 0 and keeps it there.
        //
        if (!this.enable) {
            this.lengthCounter.setCounter(0);
        }
    }

    public void lengthTick() {
        this.lengthCounter.tick();
    }

    /**
     * Waveform channel output
     *
     * @return Channel output value
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
                value = (value & 0xff00) | uint8(b);
            } else {
                value = (value & 0x00ff) | ((uint8(b) & 0x07) << 8);
            }
            this.timer.setPeriod(value);
        }

    }
}
