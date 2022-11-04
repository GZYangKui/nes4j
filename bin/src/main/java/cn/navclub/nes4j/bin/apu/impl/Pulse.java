package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.enums.ChannelType;

public class Pulse extends Channel {
    public Pulse(ChannelType type) {
        super(type);
    }

    @Override
    public void tick(int cycle) {
        if (!this.enable) {
            return;
        }
        if (this.internalTimer == 0) {
            this.internalTimer = this.timer;
            this.stop();
        } else {
            this.internalTimer--;
        }
    }
}
