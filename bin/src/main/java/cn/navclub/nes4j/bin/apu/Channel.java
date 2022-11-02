package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.CycleDriver;
import cn.navclub.nes4j.bin.enums.ChannelType;

/**
 *
 * 音频通道
 *
 */
public class Channel implements CycleDriver {
    private final ChannelType type;
    private final ChannelTimer timer;


    public Channel(ChannelType type) {
        this.type = type;
        this.timer = new ChannelTimer();
    }

    public void update(int address, byte b) {
        var pos = address % type.getOffset();
    }


    @Override
    public void tick(int cycle) {
        //驱动定时器时钟
        this.timer.tick((int) Math.round(cycle * this.type.getMultiple()));
    }
}
