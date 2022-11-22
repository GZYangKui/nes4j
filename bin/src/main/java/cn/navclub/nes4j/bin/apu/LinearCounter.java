package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Setter;

public class LinearCounter implements CycleDriver {
    private int counter;
    //禁用标识
    @Setter
    private boolean halt;
    private boolean control;
    private int reloadValue;

    //0x4008
    public void update(byte b) {
        this.reloadValue = (b & 0x7f);
        this.control = (b & 0x80) != 0;
    }

    @Override
    public void tick(int cycle) {
        if (this.halt) {
            this.counter = this.reloadValue;
        } else {
            if (this.counter != 0) {
                this.counter--;
            }
        }
        if (!this.control) {
            this.halt = false;
        }
    }
}
