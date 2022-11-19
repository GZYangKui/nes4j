package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 * 包络生成器
 */
public class Envelope implements CycleDriver {
    private final Divider divider;
    private final Channel channel;

    private int volumn;
    private int counter;
    private boolean loop;
    private boolean disable;


    public Envelope(final Channel channel) {
        this.channel = channel;
        this.divider = new Divider();
    }

    public void update(byte b) {
        this.volumn = b & 0x0f;
        this.loop = (b & 0x20) != 0;
        this.disable = (b & 0x10) != 0;
        this.divider.setPeriod(volumn + 1);
    }

    @Override
    public void tick(int cycle) {
        var lock = this.channel.lock;
        if (lock) {
            this.counter = 15;
            this.divider.reset(false);
        } else {
            this.divider.tick(cycle);
        }
        if (!this.divider.output()) {
            return;
        }
        if (this.loop && this.counter == 0)
            this.counter = 15;
        else {
            if (this.counter > 0)
                this.counter--;
        }
    }

    /**
     * 获取音量
     */
    public int volumn() {
        if (this.disable) {
            return this.volumn;
        }
        return this.counter;
    }
}
